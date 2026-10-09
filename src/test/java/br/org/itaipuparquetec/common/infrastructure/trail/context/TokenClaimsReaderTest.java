package br.org.itaipuparquetec.common.infrastructure.trail.context;

import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TokenClaimsReaderTest {
   private final TokenClaimsReader reader = new TokenClaimsReader();

   TokenClaimsReaderTest() {
   }

   @AfterEach
   void cleanSecurityContext() {
      SecurityContextHolder.clearContext();
   }

   @Test
   void shouldReadTheClaimOfTheCurrentJwt() {
      SecurityContextHolder.getContext().setAuthentication(jwtWithSubject("user-1"));
      Optional<String> claim = this.reader.claim("sub");
      Assertions.assertThat(claim).contains("user-1");
   }

   @Test
   void shouldReturnEmptyWhenTheJwtLacksTheClaim() {
      SecurityContextHolder.getContext().setAuthentication(jwtWithSubject("user-1"));
      Optional<String> claim = this.reader.claim("sid");
      Assertions.assertThat(claim).isEmpty();
   }

   @Test
   void shouldReturnEmptyWhenThereIsNoAuthentication() {
      Optional<String> claim = this.reader.claim("sub");
      Assertions.assertThat(claim).isEmpty();
   }

   @Test
   void shouldReturnEmptyWhenTheAuthenticationIsNotAJwt() {
      SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pwd"));
      Optional<String> claim = this.reader.claim("sub");
      Assertions.assertThat(claim).isEmpty();
   }

   private static JwtAuthenticationToken jwtWithSubject(String subject) {
      Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
      return new JwtAuthenticationToken(jwt);
   }
}
