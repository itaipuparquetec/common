package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.PostgresTestDatabase;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AuditOutboxTableTest {
   private final String schema = PostgresTestDatabase.newSchemaName();
   private final HikariDataSource dataSource;
   private final HikariDataSource otherConnectionPool;
   private final JdbcTemplate jdbc;
   private final AuditOutboxTable table;
   private final OutboxRows rows;

   AuditOutboxTableTest() {
      this.dataSource = PostgresTestDatabase.dataSourceWithOutboxTable(this.schema, 2);
      this.otherConnectionPool = PostgresTestDatabase.dataSourceOf(this.schema, 2);
      this.jdbc = new JdbcTemplate(this.dataSource);
      this.table = new AuditOutboxTable(this.jdbc);
      this.rows = new OutboxRows(this.jdbc);
   }

   @AfterEach
   void closePools() {
      this.dataSource.close();
      this.otherConnectionPool.close();
   }

   @Test
   void shouldLockTheOldestRowsFirstUpToTheLimitMappingEveryColumn() {
      List<UUID> ids = this.rows.insert(5);
      List<AuditOutboxRow> locked = (List)this.inTransaction(() -> this.table.lockOldest(3));
      Assertions.assertThat(locked).extracting(AuditOutboxRow::eventId).containsExactlyElementsOf(ids.subList(0, 3));
      Assertions.assertThat((AuditOutboxRow)locked.get(0)).isEqualTo(new AuditOutboxRow((UUID)ids.get(0), "1", "acme_tenant", "mirror", "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", "{\"number\":0}", 0));
   }

   @Test
   void shouldSkipTheRowsAlreadyLockedByAnotherRelayInstance() {
      List<UUID> ids = this.rows.insert(4);
      AuditOutboxTable otherInstance = new AuditOutboxTable(new JdbcTemplate(this.otherConnectionPool));
      List<AuditOutboxRow> seenByTheSecondInstance = (List)this.inTransaction(() -> {
         this.table.lockOldest(2);
         return (List)(new TransactionTemplate(new DataSourceTransactionManager(this.otherConnectionPool))).execute(status -> otherInstance.lockOldest(4));
      });
      Assertions.assertThat(seenByTheSecondInstance).extracting(AuditOutboxRow::eventId).containsExactlyElementsOf(ids.subList(2, 4));
   }

   @Test
   void shouldDeleteOnlyTheGivenRows() {
      List<UUID> ids = this.rows.insert(3);
      this.table.delete(List.of((UUID)ids.get(0), (UUID)ids.get(2)));
      Assertions.assertThat(this.rows.pendingIds()).containsExactly(new UUID[]{(UUID)ids.get(1)});
   }

   @Test
   void shouldCountAFailedAttemptOnlyOnTheGivenRows() {
      List<UUID> ids = this.rows.insert(2);
      this.table.countFailedAttempt(List.of((UUID)ids.get(1)));
      this.table.countFailedAttempt(List.of((UUID)ids.get(1)));
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(0))).isZero();
      Assertions.assertThat(this.rows.attemptsOf((UUID)ids.get(1))).isEqualTo(2);
   }

   @Test
   void shouldDoNothingWhenThereAreNoRowsToDeleteOrCount() {
      this.rows.insert(1);
      this.table.delete(List.of());
      this.table.countFailedAttempt(List.of());
      Assertions.assertThat(this.rows.pendingIds()).hasSize(1);
   }

   @Test
   void shouldCountTheBacklogUpToACap() {
      this.jdbc.update("INSERT INTO audit_outbox (event_id, schema_version, tenant, source_service, trace_id, span_id, payload) SELECT gen_random_uuid(), '1', 't', 's', 'a', 'b', 'p' FROM generate_series(1, 10100)");
      long backlog = this.table.backlog();
      Assertions.assertThat(backlog).isEqualTo(10001L);
   }

   @Test
   void shouldCountTheWholeBacklogBelowTheCap() {
      this.rows.insert(7);
      long backlog = this.table.backlog();
      Assertions.assertThat(backlog).isEqualTo(7L);
   }

   @Test
   void shouldMeasureTheAgeOfTheOldestRowInSeconds() {
      List<UUID> ids = this.rows.insert(2);
      this.jdbc.update("UPDATE audit_outbox SET created_at = now() - interval '90 seconds' WHERE event_id = ?", new Object[]{ids.get(0)});
      long age = this.table.oldestAgeSeconds();
      Assertions.assertThat(age).isBetween(90L, 120L);
   }

   @Test
   void shouldReportAgeZeroWhenTheOutboxIsEmpty() {
      long age = this.table.oldestAgeSeconds();
      Assertions.assertThat(age).isZero();
   }

   private <T> T inTransaction(Supplier<T> action) {
      return (T)(new TransactionTemplate(new DataSourceTransactionManager(this.dataSource))).execute(status -> action.get());
   }
}
