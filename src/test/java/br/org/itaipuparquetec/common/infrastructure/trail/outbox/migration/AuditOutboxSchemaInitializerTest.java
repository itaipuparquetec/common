package br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.PostgresTestDatabase;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditOutboxSchemaInitializerTest {
   private final String schema = PostgresTestDatabase.newSchemaName();
   private final HikariDataSource dataSource;

   AuditOutboxSchemaInitializerTest() {
      this.dataSource = PostgresTestDatabase.dataSourceOf(this.schema, 2);
   }

   @AfterEach
   void closePool() {
      this.dataSource.close();
   }

   @Test
   void shouldCreateTheOutboxTableInTheDatabaseOfTheServiceAtStartup() {
      AuditOutboxSchemaInitializer initializer = new AuditOutboxSchemaInitializer(new AuditOutboxMigrator(), this.dataSource);
      initializer.afterPropertiesSet();
      List<String> tables = (new JdbcTemplate(this.dataSource)).queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = ?", String.class, new Object[]{this.schema});
      Assertions.assertThat(tables).contains(new String[]{"audit_outbox"});
   }
}
