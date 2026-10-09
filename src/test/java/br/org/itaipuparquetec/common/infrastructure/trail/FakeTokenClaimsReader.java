package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.context.TokenClaimsReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class FakeTokenClaimsReader extends TokenClaimsReader {
   private final Map<String, String> claims = new HashMap();

   public static FakeTokenClaimsReader withoutToken() {
      return new FakeTokenClaimsReader();
   }

   public static FakeTokenClaimsReader ofUser(String subject) {
      return (new FakeTokenClaimsReader()).with("sub", subject).with("sid", "sid-1").with("jti", "jti-1").with("tenant_name", "acme").with("preferred_username", "jdoe");
   }

   public static FakeTokenClaimsReader ofService(String clientId) {
      return (new FakeTokenClaimsReader()).with("sub", clientId).with("jti", "jti-2").with("tenant_name", "hubti");
   }

   public FakeTokenClaimsReader with(String name, String value) {
      this.claims.put(name, value);
      return this;
   }

   @Override
   public Optional<String> claim(String name) {
      return Optional.ofNullable((String)this.claims.get(name));
   }
}
