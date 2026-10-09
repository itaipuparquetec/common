package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditEventFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.RecordingAuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantScope;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.RecordingTenantIdentifierService;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AuditOutboxNewTransactionWriterTest {
   private final FakeAuditOutboxStore store = new FakeAuditOutboxStore();
   private final FakeTransactionManager transactions = new FakeTransactionManager();
   private final RecordingAuditEventSink fallback = new RecordingAuditEventSink();
   private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
   private final RecordingTenantIdentifierService tenants = new RecordingTenantIdentifierService();
   private final AuditOutboxNewTransactionWriter writer;
   private final AuditOutboxEntry entry;

   AuditOutboxNewTransactionWriterTest() {
      this.writer = new AuditOutboxNewTransactionWriter(this.transactions, this.store, this.fallback, new AuditMetrics(this.registry, "mirror"), new ImmediateDeferredWrites(), AuditTenantScope.routingBy(this.tenants));
      this.entry = new AuditOutboxEntry(AuditEventFixture.EVENT_ID, "1", "acme_tenant", "mirror", "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", "{}");
   }

   @AfterEach
   void cleanTransactionState() {
      TransactionSynchronizationManager.clear();
   }

   @Test
   void shouldWriteTheRowInATransactionOfItsOwnAndCommit() {
      this.writer.write(this.entry, AuditEventFixture.successOf("Register"));
      Assertions.assertThat(this.store.entries()).containsExactly(new AuditOutboxEntry[]{this.entry});
      Assertions.assertThat(this.transactions.propagationsBegun()).containsExactly(new Integer[]{3});
      Assertions.assertThat(this.transactions.commits()).isEqualTo(1);
      Assertions.assertThat(this.fallback.events()).isEmpty();
   }

   @Test
   void shouldRollBackAndFallBackToTheLogWhenTheWriteFails() {
      this.store.failingWith(new IllegalStateException("database down"));
      AuditEvent event = AuditEventFixture.failureOf("Register");
      this.writer.write(this.entry, event);
      Assertions.assertThat(this.transactions.rollbacks()).isEqualTo(1);
      Assertions.assertThat(this.fallback.onlyEvent()).isSameAs(event);
      Assertions.assertThat(this.registry.counter("audit_outbox_write_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)1.0F);
   }

   @Test
   void shouldWriteRightAwayWhenThereIsNoTransactionToWaitFor() {
      this.writer.writeAfterCompletion(this.entry, AuditEventFixture.successOf("Register"));
      Assertions.assertThat(this.store.entries()).containsExactly(new AuditOutboxEntry[]{this.entry});
   }

   @Test
   void shouldWaitForTheEndOfTheBusinessTransactionBeforeWriting() {
      TransactionSynchronizationManager.initSynchronization();
      this.writer.writeAfterCompletion(this.entry, AuditEventFixture.failureOf("Register"));
      Assertions.assertThat(this.store.entries()).isEmpty();
      Assertions.assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
   }

   @Test
   void shouldWriteAfterARollbackOfTheBusinessTransaction() {
      TransactionSynchronizationManager.initSynchronization();
      this.writer.writeAfterCompletion(this.entry, AuditEventFixture.failureOf("Register"));
      completeBusinessTransaction(1);
      Assertions.assertThat(this.store.entries()).containsExactly(new AuditOutboxEntry[]{this.entry});
   }

   @Test
   void shouldWriteAfterACommitOfTheBusinessTransaction() {
      TransactionSynchronizationManager.initSynchronization();
      this.writer.writeAfterCompletion(this.entry, AuditEventFixture.successOf("Register"));
      completeBusinessTransaction(0);
      Assertions.assertThat(this.store.entries()).containsExactly(new AuditOutboxEntry[]{this.entry});
   }

   @Test
   void shouldWriteTheDeferredRowWithTheTenantOfTheEventInEffect() {
      this.writer.writeLater(this.entry, AuditEventFixture.successOf("Register"));
      Assertions.assertThat(this.store.entries()).containsExactly(new AuditOutboxEntry[]{this.entry});
      Assertions.assertThat(this.tenants.calls()).containsExactly(new String[]{"set:acme_tenant", "clear"});
   }

   @Test
   void shouldFallBackToTheLogWhenTheDeferredWritesAreSaturated() {
      AuditOutboxNewTransactionWriter saturated = new AuditOutboxNewTransactionWriter(this.transactions, this.store, this.fallback, new AuditMetrics(this.registry, "mirror"), new SaturatedDeferredWrites(), AuditTenantScope.none());
      AuditEvent event = AuditEventFixture.failureOf("Register");
      saturated.writeLater(this.entry, event);
      Assertions.assertThat(this.store.entries()).isEmpty();
      Assertions.assertThat(this.fallback.onlyEvent()).isSameAs(event);
      Assertions.assertThat(this.registry.counter("audit_outbox_write_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)1.0F);
   }

   @Test
   void shouldCountAndLogTheEventWhenFallingBack() {
      AuditEvent event = AuditEventFixture.successOf("Register");
      this.writer.fallBackToLog(event);
      Assertions.assertThat(this.fallback.onlyEvent()).isSameAs(event);
      Assertions.assertThat(this.registry.counter("audit_outbox_write_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)1.0F);
   }

   private static void completeBusinessTransaction(int status) {
      TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCompletion(status));
   }
}
