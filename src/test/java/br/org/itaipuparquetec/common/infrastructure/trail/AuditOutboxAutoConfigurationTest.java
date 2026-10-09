package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantScope;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.RecordingTenantIdentifierService;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxStore;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.FakeTransactionManager;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.JpaAuditOutboxStore;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.ThreadPoolDeferredWrites;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.OutboxAuditEventSink;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.ApplicationContextAssert;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.Mockito.mock;

class AuditOutboxAutoConfigurationTest {
   private final ApplicationContextRunner runner = (new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(AuditTrailAutoConfiguration.class, AuditOutboxAutoConfiguration.class)).withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class), new BeanDefinitionCustomizer[0]).withBean(PlatformTransactionManager.class, FakeTransactionManager::new, new BeanDefinitionCustomizer[0]).withPropertyValues(new String[]{"audit.enabled=true", "audit.sink=outbox", "spring.application.name=mirror"});

   AuditOutboxAutoConfigurationTest() {
   }

   @Test
   void shouldWireTheOutboxSinkWhenTheOutboxSinkIsSelected() {
      this.runner.run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditEventSink.class);
         Assertions.assertThat((AuditEventSink)context.getBean(AuditEventSink.class)).isInstanceOf(OutboxAuditEventSink.class);
         Assertions.assertThat((AuditOutboxStore)context.getBean(AuditOutboxStore.class)).isInstanceOf(JpaAuditOutboxStore.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditMetrics.class);
      });
   }

   @Test
   void shouldNotWireTheOutboxWhenTheLogSinkIsSelected() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.sink=log"})).run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(OutboxAuditEventSink.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditOutboxStore.class);
      });
   }

   @Test
   void shouldNotWireTheOutboxWhenTheTrailIsDisabled() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.enabled=false"})).run(context -> ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditOutboxStore.class));
   }

   @Test
   void shouldKeepTheDeferredWritersOutOfTheExecutorBeansSoTheTaskExecutorOfSpringBootIsUntouched() {
      this.runner.run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(ThreadPoolDeferredWrites.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(Executor.class);
      });
   }

   @Test
   void shouldRecordMetricsInTheMeterRegistryOfTheApplicationWhenThereIsOne() {
      SimpleMeterRegistry registry = new SimpleMeterRegistry();
      ((ApplicationContextRunner)this.runner.withBean(MeterRegistry.class, () -> registry, new BeanDefinitionCustomizer[0])).run(context -> {
         ((AuditMetrics)context.getBean(AuditMetrics.class)).outboxWriteFailed();
         Assertions.assertThat(registry.counter("audit_outbox_write_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)1.0F);
      });
   }

   @Test
   void shouldRecordMetricsInAPrivateRegistryWhenThereIsNone() {
      this.runner.run(context -> Assertions.assertThat((AuditMetrics)context.getBean(AuditMetrics.class)).isNotNull());
   }

   @Test
   void shouldRouteTheDeferredWritesByTenantWhenTheServiceUsesDatabasePerTenant() {
      RecordingTenantIdentifierService tenants = new RecordingTenantIdentifierService();
      ((ApplicationContextRunner)this.runner.withBean(TenantIdentifierService.class, () -> tenants, new BeanDefinitionCustomizer[0])).run(context -> {
         ((AuditTenantScope)context.getBean(AuditTenantScope.class)).runAs("acme_tenant", () -> {
         });
         Assertions.assertThat(tenants.calls()).containsExactly(new String[]{"set:acme_tenant", "clear"});
      });
   }

   @Test
   void shouldJustRunTheDeferredWritesWhenTheServiceHasASingleDatabase() {
      this.runner.run(context -> {
         AtomicBoolean ran = new AtomicBoolean();
         ((AuditTenantScope)context.getBean(AuditTenantScope.class)).runAs("hubti", () -> ran.set(true));
         Assertions.assertThat(ran).isTrue();
      });
   }
}
