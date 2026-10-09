package br.org.itaipuparquetec.common.infrastructure.trail.identity;

import java.util.ArrayList;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AuditTenantScopeTest {
   private final RecordingTenantIdentifierService tenants = new RecordingTenantIdentifierService();

   AuditTenantScopeTest() {
   }

   @Test
   void shouldRunTheActionWithTheTenantInEffectAndClearItAfterwards() {
      ArrayList<String> seen = new ArrayList();
      AuditTenantScope.routingBy(this.tenants).runAs("acme_tenant", () -> seen.add(this.tenants.resolveCurrentTenantIdentifier()));
      Assertions.assertThat(seen).containsExactly(new String[]{"acme_tenant"});
      Assertions.assertThat(this.tenants.calls()).containsExactly(new String[]{"set:acme_tenant", "clear"});
   }

   @Test
   void shouldClearTheTenantEvenWhenTheActionFails() {
      AuditTenantScope scope = AuditTenantScope.routingBy(this.tenants);
      Assertions.assertThatThrownBy(() -> scope.runAs("acme_tenant", () -> {
            throw new IllegalStateException("write failed");
         })).isInstanceOf(IllegalStateException.class);
      Assertions.assertThat(this.tenants.calls()).containsExactly(new String[]{"set:acme_tenant", "clear"});
   }

   @Test
   void shouldJustRunTheActionWhenThereIsNoTenantRouting() {
      ArrayList<String> ran = new ArrayList();
      AuditTenantScope.none().runAs("acme_tenant", () -> ran.add("ran"));
      Assertions.assertThat(ran).containsExactly(new String[]{"ran"});
   }
}
