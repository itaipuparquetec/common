package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.RecordingTenantIdentifierService;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.SourceIdentity;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.Slf4jAuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.MicrometerUseCaseTracing;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.StandaloneUseCaseTracing;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.UseCaseTracing;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.Properties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuditTrailAutoConfigurationWiringTest {
   private final ApplicationContextRunner runner = (ApplicationContextRunner)((ApplicationContextRunner)(new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(new Class[]{AuditTrailAutoConfiguration.class}))).withPropertyValues(new String[]{"audit.enabled=true", "spring.application.name=mirror"});

   AuditTrailAutoConfigurationWiringTest() {
   }

   @Test
   void shouldBindTheDefaultsOfTheTrailSettings() {
      this.runner.run(context -> {
         AuditProperties properties = (AuditProperties)context.getBean(AuditProperties.class);
         Assertions.assertThat(properties.sink()).isEqualTo(AuditSinkType.LOG);
         Assertions.assertThat(properties.includeReadOnly()).isFalse();
         Assertions.assertThat(properties.maxInputBytes()).isEqualTo(32768);
         Assertions.assertThat(properties.topics().events()).isEqualTo("hubti.trail.events");
         Assertions.assertThat(properties.topics().deadLetter()).isEqualTo("hubti.trail.events.dlt");
         Assertions.assertThat(properties.outbox().migrateSchema()).isTrue();
         Assertions.assertThat(properties.outbox().deferredWriterThreads()).isEqualTo(2);
         Assertions.assertThat(properties.outbox().deferredQueueCapacity()).isEqualTo(10000);
         Assertions.assertThat(properties.relay().enabled()).isFalse();
         Assertions.assertThat(properties.relay().interval()).isEqualTo(Duration.ofSeconds(1L));
         Assertions.assertThat(properties.relay().batchSize()).isEqualTo(100);
         Assertions.assertThat(properties.relay().sendTimeout()).isEqualTo(Duration.ofSeconds(30L));
         Assertions.assertThat(properties.relay().tenantRefresh()).isEqualTo(Duration.ofSeconds(60L));
         Assertions.assertThat(properties.relay().alertAttempts()).isEqualTo(10);
      });
   }

   @Test
   void shouldBindTheTrailSettingsFromTheEnvironment() {
      ((ApplicationContextRunner)((ApplicationContextRunner)this.runner.withBean(AuditEventSink.class, RecordingAuditEventSink::new, new BeanDefinitionCustomizer[0])).withPropertyValues(new String[]{"audit.sink=outbox", "audit.include-read-only=true", "audit.max-input-bytes=1024", "audit.topics.events=custom.events", "audit.topics.dead-letter=custom.dlt", "audit.relay.enabled=true", "audit.relay.interval=5s", "audit.relay.batch-size=7"})).run(context -> {
         AuditProperties properties = (AuditProperties)context.getBean(AuditProperties.class);
         Assertions.assertThat(properties.sink()).isEqualTo(AuditSinkType.OUTBOX);
         Assertions.assertThat(properties.includeReadOnly()).isTrue();
         Assertions.assertThat(properties.maxInputBytes()).isEqualTo(1024);
         Assertions.assertThat(properties.topics().events()).isEqualTo("custom.events");
         Assertions.assertThat(properties.topics().deadLetter()).isEqualTo("custom.dlt");
         Assertions.assertThat(properties.relay().enabled()).isTrue();
         Assertions.assertThat(properties.relay().interval()).isEqualTo(Duration.ofSeconds(5L));
         Assertions.assertThat(properties.relay().batchSize()).isEqualTo(7);
      });
   }

   @Test
   void shouldMigrateTheOutboxSchemaOnlyForTheEnabledOutboxSink() {
      ApplicationContextRunner withCustomSink = (ApplicationContextRunner)this.runner.withBean(AuditEventSink.class, RecordingAuditEventSink::new, new BeanDefinitionCustomizer[0]);
      ((ApplicationContextRunner)withCustomSink.withPropertyValues(new String[]{"audit.sink=outbox"})).run(context -> Assertions.assertThat(((AuditProperties)context.getBean(AuditProperties.class)).migratesOutboxSchema()).isTrue());
      ((ApplicationContextRunner)withCustomSink.withPropertyValues(new String[]{"audit.sink=outbox", "audit.outbox.migrate-schema=false"})).run(context -> Assertions.assertThat(((AuditProperties)context.getBean(AuditProperties.class)).migratesOutboxSchema()).isFalse());
      this.runner.run(context -> Assertions.assertThat(((AuditProperties)context.getBean(AuditProperties.class)).migratesOutboxSchema()).isFalse());
   }

   @Test
   void shouldUseTheLogSinkByDefaultAndWhenTheLogSinkIsSelected() {
      this.runner.run(context -> Assertions.assertThat((AuditEventSink)context.getBean(AuditEventSink.class)).isInstanceOf(Slf4jAuditEventSink.class));
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.sink=log"})).run(context -> Assertions.assertThat((AuditEventSink)context.getBean(AuditEventSink.class)).isInstanceOf(Slf4jAuditEventSink.class));
   }

   @Test
   void shouldLeaveTheSinkToTheOutboxConfigurationWhenTheOutboxSinkIsSelected() {
      ((ApplicationContextRunner)((ApplicationContextRunner)this.runner.withBean(AuditEventSink.class, RecordingAuditEventSink::new, new BeanDefinitionCustomizer[0])).withPropertyValues(new String[]{"audit.sink=outbox"})).run(context -> Assertions.assertThat((AuditEventSink)context.getBean(AuditEventSink.class)).isInstanceOf(RecordingAuditEventSink.class));
   }

   @Test
   void shouldOpenSpansThroughMicrometerTracingWhenATracerIsAvailable() {
      ((ApplicationContextRunner)this.runner.withBean(Tracer.class, () -> Tracer.NOOP, new BeanDefinitionCustomizer[0])).run(context -> Assertions.assertThat((UseCaseTracing)context.getBean(UseCaseTracing.class)).isInstanceOf(MicrometerUseCaseTracing.class));
   }

   @Test
   void shouldGenerateW3cIdentifiersLocallyWhenThereIsNoTracer() {
      this.runner.run(context -> Assertions.assertThat((UseCaseTracing)context.getBean(UseCaseTracing.class)).isInstanceOf(StandaloneUseCaseTracing.class));
   }

   @Test
   void shouldTakeTheSourceFromTheApplicationNameAndTheConfiguredVersion() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.source-version=0.8.0"})).run(context -> Assertions.assertThat((SourceIdentity)context.getBean(SourceIdentity.class)).isEqualTo(new SourceIdentity("mirror", "0.8.0")));
   }

   @Test
   void shouldPreferTheBuildInformationForTheSourceVersion() {
      Properties build = new Properties();
      build.setProperty("version", "1.2.3");
      ((ApplicationContextRunner)((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.source-version=0.8.0"})).withBean(BuildProperties.class, () -> new BuildProperties(build), new BeanDefinitionCustomizer[0])).run(context -> Assertions.assertThat((SourceIdentity)context.getBean(SourceIdentity.class)).isEqualTo(new SourceIdentity("mirror", "1.2.3")));
   }

   @Test
   void shouldLeaveTheSourceVersionUnknownWhenNothingProvidesIt() {
      this.runner.run(context -> Assertions.assertThat(((SourceIdentity)context.getBean(SourceIdentity.class)).version()).isNull());
   }

   @Test
   void shouldResolveTheTenantInEffectWhenTheServiceRoutesByTenant() {
      RecordingTenantIdentifierService tenants = new RecordingTenantIdentifierService();
      tenants.setTenantId("acme_tenant");
      ((ApplicationContextRunner)this.runner.withBean(TenantIdentifierService.class, () -> tenants, new BeanDefinitionCustomizer[0])).run(context -> Assertions.assertThat(((AuditTenantResolver)context.getBean(AuditTenantResolver.class)).current()).isEqualTo("acme_tenant"));
   }

   @Test
   void shouldResolveTheDefaultTenantWhenTheServiceHasASingleDatabase() {
      this.runner.run(context -> Assertions.assertThat(((AuditTenantResolver)context.getBean(AuditTenantResolver.class)).current()).isEqualTo("hubti"));
   }
}
