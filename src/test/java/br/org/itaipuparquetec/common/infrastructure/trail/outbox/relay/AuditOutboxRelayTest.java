package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditProperties;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditPropertiesFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.PostgresTestDatabase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditOutboxRelayTest {
   private final String schema = PostgresTestDatabase.newSchemaName();
   private final HikariDataSource dataSource;
   private final JdbcTemplate jdbc;
   private final OutboxRows rows;
   private final SimpleMeterRegistry registry;
   private final AuditMetrics metrics;
   private final ScriptedProducer producer;
   private final Logger relayLogger;
   private final ListAppender<ILoggingEvent> logs;

   AuditOutboxRelayTest() {
      this.dataSource = PostgresTestDatabase.dataSourceWithOutboxTable(this.schema, 4);
      this.jdbc = new JdbcTemplate(this.dataSource);
      this.rows = new OutboxRows(this.jdbc);
      this.registry = new SimpleMeterRegistry();
      this.metrics = new AuditMetrics(this.registry, "mirror");
      this.producer = new ScriptedProducer();
      this.relayLogger = (Logger)LoggerFactory.getLogger(AuditOutboxRelay.class);
      this.logs = new ListAppender();
   }

   @BeforeEach
   void captureLogs() {
      this.logs.start();
      this.relayLogger.addAppender(this.logs);
   }

   @AfterEach
   void releaseResources() {
      this.relayLogger.detachAppender(this.logs);
      this.dataSource.close();
   }

   @Test
   void shouldPublishEveryPendingRowKeyedByTraceIdAndDeleteThemAfterTheAcknowledgement() {
      List<UUID> ids = this.rows.insert(3);
      this.relayWith(this.producer, 10).relayAllTenants();
      Assertions.assertThat(this.producer.history()).hasSize(3);
      ProducerRecord<String, String> producedRecord = (ProducerRecord)this.producer.history().get(0);
      Assertions.assertThat(producedRecord.topic()).isEqualTo("hubti.trail.events");
      Assertions.assertThat((String)producedRecord.key()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
      Assertions.assertThat((String)producedRecord.value()).isEqualTo("{\"number\":0}");
      Assertions.assertThat(this.producer.headerOf(producedRecord, "trail-event-id")).isEqualTo(((UUID)ids.get(0)).toString());
      Assertions.assertThat(this.rows.pendingIds()).isEmpty();
      Assertions.assertThat(this.registry.counter("audit_outbox_published", new String[]{"service", "mirror"}).count()).isEqualTo((double)3.0F);
   }

   @Test
   void shouldPublishAtMostTheBatchSizePerCycle() {
      this.rows.insert(15);
      this.relayWith(this.producer, 10).relayAllTenants();
      Assertions.assertThat(this.producer.history()).hasSize(10);
      Assertions.assertThat(this.rows.pendingIds()).hasSize(5);
   }

   @Test
   void shouldKeepTheRowsTheBrokerRejectedCountingTheAttemptAndDeleteTheOthers() {
      List<UUID> ids = this.rows.insert(3);
      this.producer.rejecting(ids.get(1));
      this.relayWith(this.producer, 10).relayAllTenants();
      Assertions.assertThat(this.rows.pendingIds()).containsExactly(new UUID[]{(UUID)ids.get(1)});
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(1))).isEqualTo(1);
      Assertions.assertThat(this.registry.counter("audit_outbox_publish_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)1.0F);
   }

   @Test
   void shouldKeepSendingTheOtherRowsWhenOneRowCannotBeHandedToTheProducer() {
      List<UUID> ids = this.rows.insert(3);
      this.producer.failingToSend(ids.get(1));
      this.relayWith(this.producer, 10).relayAllTenants();
      Assertions.assertThat(this.rows.pendingIds()).containsExactly(new UUID[]{(UUID)ids.get(1)});
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(1))).isEqualTo(1);
      Assertions.assertThat(this.producer.history()).hasSize(2);
   }

   @Test
   void shouldStopSendingTheBatchWhenTheProducerCannotReachTheCluster() {
      List<UUID> ids = this.rows.insert(4);
      this.producer.unreachableFor(ids.get(1));
      this.relayWith(this.producer, 10).relayAllTenants();
      Assertions.assertThat(this.rows.pendingIds()).containsExactly(new UUID[]{(UUID)ids.get(1), (UUID)ids.get(2), (UUID)ids.get(3)});
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(1))).isEqualTo(1);
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(2))).isZero();
      Assertions.assertThat(this.producer.history()).hasSize(1);
   }

   @Test
   void shouldKeepTheRowWhenTheBrokerDoesNotAcknowledgeBeforeTheTimeout() {
      List<UUID> ids = this.rows.insert(2);
      this.producer.neverAcknowledging(ids.get(0));
      this.relayWith(this.producer, 10).relayAllTenants();
      Assertions.assertThat(this.rows.pendingIds()).containsExactly(new UUID[]{(UUID)ids.get(0)});
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(0))).isEqualTo(1);
   }

   @Test
   void shouldAlertWhenARowReachesTheAttemptsThreshold() {
      List<UUID> ids = this.rows.insert(1);
      this.producer.rejecting(ids.get(0));
      AuditOutboxRelay relay = this.relayWith(this.producer, 10);
      relay.relayAllTenants();
      relay.relayAllTenants();
      ((ListAssert)Assertions.assertThat(this.logs.list).filteredOn(log -> log.getLevel() == Level.ERROR)).isEmpty();
      relay.relayAllTenants();
      ((ListAssert)Assertions.assertThat(this.logs.list).filteredOn(log -> log.getLevel() == Level.ERROR)).hasSize(1);
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(0))).isEqualTo(3);
   }

   @Test
   void shouldObserveTheBacklogAndTheAgeOfTheOldestRowAfterTheCycle() {
      List<UUID> ids = this.rows.insert(2);
      this.producer.rejecting(ids.get(0));
      this.relayWith(this.producer, 10).relayAllTenants();
      RequiredSearch backlog = this.registry.get("audit_outbox_backlog").tags(new String[]{"service", "mirror", "tenant", "acme_tenant"});
      Assertions.assertThat(backlog.gauge().value()).isEqualTo((double)1.0F);
   }

   @Test
   void shouldNeverPublishRowsLockedByAnotherRelayInstance() throws Exception {
      List<UUID> ids = this.rows.insert(4);
      CountDownLatch gate = new CountDownLatch(1);
      ScriptedProducer blockedProducer = (new ScriptedProducer()).blockingUntil(gate);
      Thread firstInstance = new Thread(() -> this.relayWith(blockedProducer, 2).relayAllTenants());
      firstInstance.start();
      Assertions.assertThat(blockedProducer.awaitSendAttempt(Duration.ofSeconds(5L))).isTrue();
      this.relayWith(this.producer, 4).relayAllTenants();
      Assertions.assertThat(this.producer.history()).extracting(producedRecord -> this.producer.headerOf(producedRecord, "trail-event-id")).containsExactly(new String[]{((UUID)ids.get(2)).toString(), ((UUID)ids.get(3)).toString()});
      gate.countDown();
      firstInstance.join(TimeUnit.SECONDS.toMillis(5L));
      Assertions.assertThat(this.rows.pendingIds()).isEmpty();
   }

   @Test
   void shouldKeepRelayingTheOtherTenantsWhenOneTenantFails() {
      this.rows.insert(1);
      HikariDataSource broken = PostgresTestDatabase.dataSourceOf(PostgresTestDatabase.newSchemaName(), 1);
      FakeOutboxCatalog catalog = (new FakeOutboxCatalog()).with("broken_tenant", broken).with("acme_tenant", this.dataSource);
      AuditOutboxRelay relay = new AuditOutboxRelay(catalog, new KafkaAuditOutboxPublisher(this.producer, "hubti.trail.events"), this.metrics, AuditPropertiesFixture.relay());
      relay.relayAllTenants();
      Assertions.assertThat(this.rows.pendingIds()).isEmpty();
      ((ListAssert)Assertions.assertThat(this.logs.list).filteredOn(log -> log.getLevel() == Level.WARN)).hasSize(1);
      broken.close();
   }

   private AuditOutboxRelay relayWith(ScriptedProducer kafka, int batchSize) {
      AuditProperties.Relay settings = new AuditProperties.Relay(true, Duration.ofMillis(50L), batchSize, Duration.ofMillis(300L), Duration.ofSeconds(60L), 3);
      SingleDatabaseOutboxCatalog catalog = new SingleDatabaseOutboxCatalog("acme_tenant", this.dataSource);
      return new AuditOutboxRelay(catalog, new KafkaAuditOutboxPublisher(kafka, "hubti.trail.events"), this.metrics, settings);
   }
}
