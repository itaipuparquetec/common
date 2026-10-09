package br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.PostgresTestDatabase;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditOutboxMigratorTest {
   private final String schema = PostgresTestDatabase.newSchemaName();
   private final HikariDataSource dataSource;
   private final JdbcTemplate jdbc;
   private final AuditOutboxMigrator migrator;

   AuditOutboxMigratorTest() {
      this.dataSource = PostgresTestDatabase.dataSourceOf(this.schema, 2);
      this.jdbc = new JdbcTemplate(this.dataSource);
      this.migrator = new AuditOutboxMigrator();
   }

   @AfterEach
   void closePool() {
      this.dataSource.close();
   }

   @Test
   void shouldCreateTheOutboxTableInTheSchemaOfTheService() {
      this.migrator.migrate(this.dataSource, this.schema);
      Assertions.assertThat(this.columnsOfOutbox()).containsExactlyInAnyOrder(new String[]{"event_id", "schema_version", "tenant", "source_service", "trace_id", "span_id", "payload", "created_at", "attempts"});
   }

   @Test
   void shouldKeepItsOwnHistoryTableAwayFromTheMigrationsOfTheService() {
      this.migrator.migrate(this.dataSource, this.schema);
      ((ListAssert)Assertions.assertThat(this.tables()).contains(new String[]{"audit_outbox", "audit_flyway_history"})).doesNotContain(new String[]{"flyway_schema_history"});
   }

   @Test
   void shouldBeIdempotentWhenMigratedTwice() {
      this.migrator.migrate(this.dataSource, this.schema);
      Assertions.assertThatCode(() -> this.migrator.migrate(this.dataSource, this.schema)).doesNotThrowAnyException();
   }

   @Test
   void shouldCoexistWithTablesAndAMigrationHistoryOfTheService() {
      this.jdbc.execute("CREATE TABLE flyway_schema_history (installed_rank int)");
      this.jdbc.execute("CREATE TABLE orders (id int)");
      this.migrator.migrate(this.dataSource, this.schema);
      Assertions.assertThat(this.tables()).contains(new String[]{"audit_outbox", "orders", "flyway_schema_history"});
   }

   @Test
   void shouldUseTheSchemaOfTheConnectionWhenNoSchemaIsGiven() {
      this.migrator.migrate(this.dataSource, (String)null);
      Assertions.assertThat(this.columnsOfOutbox()).contains(new String[]{"event_id"});
   }

   private List<String> columnsOfOutbox() {
      return this.jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = 'audit_outbox'", String.class, new Object[]{this.schema});
   }

   private List<String> tables() {
      return this.jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = ?", String.class, new Object[]{this.schema});
   }
}
