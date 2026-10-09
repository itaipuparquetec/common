package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.outbox.PostgresTestDatabase;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.migration.AuditOutboxSchemaInitializer;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.ApplicationContextAssert;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditOutboxMigrationAutoConfigurationTest {
   private final String schema = PostgresTestDatabase.newSchemaName();
   private final HikariDataSource dataSource;
   private final ApplicationContextRunner runner;

   AuditOutboxMigrationAutoConfigurationTest() {
      this.dataSource = PostgresTestDatabase.dataSourceOf(this.schema, 2);
      this.runner = (ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)(new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(new Class[]{AuditOutboxMigrationAutoConfiguration.class}))).withBean(DataSource.class, () -> this.dataSource, new BeanDefinitionCustomizer[0])).withPropertyValues(new String[]{"audit.enabled=true", "audit.sink=outbox"});
   }

   @AfterEach
   void closePool() {
      this.dataSource.close();
   }

   @Test
   void shouldCreateTheOutboxTableAtStartupOfAServiceWithASingleDatabase() {
      this.runner.run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditOutboxSchemaInitializer.class);
         Assertions.assertThat(this.tables()).contains(new String[]{"audit_outbox"});
      });
   }

   @Test
   void shouldNotMigrateWhenTheSchemaMigrationIsDisabled() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.outbox.migrate-schema=false"})).run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditOutboxSchemaInitializer.class);
         Assertions.assertThat(this.tables()).doesNotContain(new String[]{"audit_outbox"});
      });
   }

   @Test
   void shouldNotMigrateWhenTheSinkIsNotTheOutbox() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.sink=log"})).run(context -> ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditOutboxSchemaInitializer.class));
   }

   @Test
   void shouldLeaveTheMigrationToTheTenantMigrationServiceWhenTheServiceUsesDatabasePerTenant() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"hubti.multitenancy.enabled=true"})).run(context -> ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditOutboxSchemaInitializer.class));
   }

   private List<String> tables() {
      return (new JdbcTemplate(this.dataSource)).queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = ?", String.class, new Object[]{this.schema});
   }
}
