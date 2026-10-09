package br.org.itaipuparquetec.common.infrastructure.trail.identity;

import br.org.itaipuparquetec.common.infrastructure.trail.FakeTokenClaimsReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AuditActorResolverTest {
   AuditActorResolverTest() {
   }

   @Test
   void shouldResolveAUserFromATokenIssuedToAPerson() {
      AuditActorResolver resolver = new AuditActorResolver(FakeTokenClaimsReader.ofUser("user-1"));
      AuditActor actor = resolver.current();
      Assertions.assertThat(actor).isEqualTo(new AuditActor("user-1", "sid-1", "jti-1", "acme", AuditActorType.USER));
   }

   @Test
   void shouldResolveAUserFromATokenWithOnlyAUsername() {
      FakeTokenClaimsReader claims = (new FakeTokenClaimsReader()).with("sub", "user-1").with("preferred_username", "jdoe");
      AuditActorResolver resolver = new AuditActorResolver(claims);
      AuditActor actor = resolver.current();
      Assertions.assertThat(actor.type()).isEqualTo(AuditActorType.USER);
   }

   @Test
   void shouldResolveAServiceFromAClientCredentialsToken() {
      AuditActorResolver resolver = new AuditActorResolver(FakeTokenClaimsReader.ofService("hubti-service"));
      AuditActor actor = resolver.current();
      Assertions.assertThat(actor).isEqualTo(new AuditActor("hubti-service", (String)null, "jti-2", "hubti", AuditActorType.SERVICE));
   }

   @Test
   void shouldResolveTheSystemWhenThereIsNoToken() {
      AuditActorResolver resolver = new AuditActorResolver(FakeTokenClaimsReader.withoutToken());
      AuditActor actor = resolver.current();
      Assertions.assertThat(actor).isEqualTo(new AuditActor((String)null, (String)null, (String)null, (String)null, AuditActorType.SYSTEM));
   }
}
