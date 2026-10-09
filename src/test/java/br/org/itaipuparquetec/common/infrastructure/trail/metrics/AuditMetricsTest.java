package br.org.itaipuparquetec.common.infrastructure.trail.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AuditMetricsTest {
   private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
   private final AuditMetrics metrics;

   AuditMetricsTest() {
      this.metrics = new AuditMetrics(this.registry, "mirror");
   }

   @Test
   void shouldCountTheOutboxWriteFailuresOfTheService() {
      this.metrics.outboxWriteFailed();
      this.metrics.outboxWriteFailed();
      double count = this.registry.counter("audit_outbox_write_failures", new String[]{"service", "mirror"}).count();
      Assertions.assertThat(count).isEqualTo((double)2.0F);
   }

   @Test
   void shouldCountThePublishedAndTheFailedPublications() {
      this.metrics.published(5);
      this.metrics.publishFailed(2);
      Assertions.assertThat(this.registry.counter("audit_outbox_published", new String[]{"service", "mirror"}).count()).isEqualTo((double)5.0F);
      Assertions.assertThat(this.registry.counter("audit_outbox_publish_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)2.0F);
   }

   @Test
   void shouldExposeTheBacklogAndTheOldestAgePerTenantAndKeepTheLatestObservation() {
      this.metrics.observeBacklog("acme_tenant", 10L, 30L);
      this.metrics.observeBacklog("acme_tenant", 4L, 12L);
      this.metrics.observeBacklog("hubti", 1L, 2L);
      Assertions.assertThat(this.gauge("audit_outbox_backlog", "acme_tenant")).isEqualTo((double)4.0F);
      Assertions.assertThat(this.gauge("audit_outbox_oldest_age_seconds", "acme_tenant")).isEqualTo((double)12.0F);
      Assertions.assertThat(this.gauge("audit_outbox_backlog", "hubti")).isEqualTo((double)1.0F);
   }

   private double gauge(String name, String tenant) {
      return this.registry.get(name).tags(new String[]{"service", "mirror", "tenant", tenant}).gauge().value();
   }
}
