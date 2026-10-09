package br.org.itaipuparquetec.common.infrastructure.trail.identity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AuditTenantResolverTest {
   AuditTenantResolverTest() {
   }

   @Test
   void shouldResolveTheTenantInEffect() {
      AuditTenantResolver resolver = new AuditTenantResolver(() -> "acme_tenant");
      String tenant = resolver.current();
      Assertions.assertThat(tenant).isEqualTo("acme_tenant");
   }

   @Test
   void shouldAlwaysResolveTheDefaultTenantInASingleTenantService() {
      AuditTenantResolver resolver = AuditTenantResolver.singleTenant();
      String tenant = resolver.current();
      Assertions.assertThat(tenant).isEqualTo("hubti");
   }
}
