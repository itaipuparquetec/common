package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.PostgreSQLMigrationServiceImpl;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.FakeTransactionManager;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.AuditOutboxCatalog;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.AuditOutboxRelay;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.AuditOutboxRelayLifecycle;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.KafkaAuditOutboxPublisher;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.MultitenantOutboxCatalog;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay.SingleDatabaseOutboxCatalog;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.assertj.ApplicationContextAssert;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.Mockito.mock;

class AuditOutboxRelayAutoConfigurationTest {
   private final ApplicationContextRunner runner = (ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)(new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(new Class[]{AuditTrailAutoConfiguration.class, AuditOutboxAutoConfiguration.class, AuditOutboxRelayAutoConfiguration.class}))).withBean(EntityManagerFactory.class, () -> (EntityManagerFactory) mock(EntityManagerFactory.class), new BeanDefinitionCustomizer[0])).withBean(PlatformTransactionManager.class, FakeTransactionManager::new, new BeanDefinitionCustomizer[0])).withBean(DataSource.class, () -> (DataSource) mock(DataSource.class), new BeanDefinitionCustomizer[0])).withBean(KafkaProperties.class, AuditOutboxRelayAutoConfigurationTest::kafkaProperties, new BeanDefinitionCustomizer[0])).withPropertyValues(new String[]{"audit.enabled=true", "audit.sink=outbox", "audit.relay.enabled=true", "audit.relay.interval=1h", "spring.application.name=mirror"});

   AuditOutboxRelayAutoConfigurationTest() {
   }

   @Test
   void shouldWireTheRelayWithADedicatedProducerWhenTheRelayIsEnabled() {
      this.runner.run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(KafkaAuditOutboxPublisher.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditOutboxRelay.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditOutboxRelayLifecycle.class);
         Assertions.assertThat(((AuditOutboxRelayLifecycle)context.getBean(AuditOutboxRelayLifecycle.class)).isRunning()).isTrue();
      });
   }

   @Test
   void shouldNotWireTheRelayByDefault() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.relay.enabled=false"})).run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditOutboxRelay.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(KafkaAuditOutboxPublisher.class);
      });
   }

   @Test
   void shouldNotWireTheRelayWhenTheSinkIsNotTheOutbox() {
      this.runner.withPropertyValues(new String[]{"audit.sink=log"}).run(context -> ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditOutboxRelay.class));
   }

   @Test
   void shouldRelayTheSingleDatabaseOfAServiceWithoutMultitenancy() {
      this.runner.run(context -> Assertions.assertThat(context.getBean(AuditOutboxCatalog.class)).isInstanceOf(SingleDatabaseOutboxCatalog.class));
   }

   @Test
   void shouldRelayEveryTenantDatabaseWhenTheServiceUsesDatabasePerTenant() {
      this.runner.withPropertyValues(new String[]{"hubti.multitenancy.enabled=true"}).withBean(PostgreSQLMigrationServiceImpl.class, () -> mock(PostgreSQLMigrationServiceImpl.class), new BeanDefinitionCustomizer[0]).withBean(TenantDataSourceRegistryImpl.class, () -> mock(TenantDataSourceRegistryImpl.class), new BeanDefinitionCustomizer[0]).run(context -> Assertions.assertThat(context.getBean(AuditOutboxCatalog.class)).isInstanceOf(MultitenantOutboxCatalog.class));
   }

   private static KafkaProperties kafkaProperties() {
      KafkaProperties properties = new KafkaProperties();
      properties.setBootstrapServers(List.of("localhost:1"));
      return properties;
   }
}
