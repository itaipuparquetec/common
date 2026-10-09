package br.org.itaipuparquetec.common.infrastructure.trail.serialization;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class PseudonymizerTest {
   PseudonymizerTest() {
   }

   @Test
   void shouldReturnNullForANullValue() {
      Pseudonymizer pseudonymizer = new Pseudonymizer("salt");
      String pseudonym = pseudonymizer.pseudonymize((Object)null);
      Assertions.assertThat(pseudonym).isNull();
   }

   @Test
   void shouldBeDeterministicForTheSameValueAndSalt() {
      Pseudonymizer pseudonymizer = new Pseudonymizer("salt");
      String first = pseudonymizer.pseudonymize("joao@example.com");
      String second = pseudonymizer.pseudonymize("joao@example.com");
      Assertions.assertThat(first).isEqualTo(second).matches("anon:[0-9a-f]{16}");
   }

   @Test
   void shouldNotExposeTheOriginalValue() {
      Pseudonymizer pseudonymizer = new Pseudonymizer("salt");
      String pseudonym = pseudonymizer.pseudonymize("joao@example.com");
      Assertions.assertThat(pseudonym).doesNotContain(new CharSequence[]{"joao"});
   }

   @Test
   void shouldProduceDifferentPseudonymsForDifferentSalts() {
      String first = (new Pseudonymizer("salt-a")).pseudonymize("value");
      String second = (new Pseudonymizer("salt-b")).pseudonymize("value");
      Assertions.assertThat(first).isNotEqualTo(second);
   }

   @ParameterizedTest
   @NullSource
   @ValueSource(
      strings = {""}
   )
   void shouldTreatANullSaltAsEmpty(String salt) {
      String withNullOrEmpty = (new Pseudonymizer(salt)).pseudonymize("value");
      String withEmpty = (new Pseudonymizer("")).pseudonymize("value");
      Assertions.assertThat(withNullOrEmpty).isEqualTo(withEmpty);
   }

   @Test
   void shouldFailWithAnExplicitMessageWhenTheDigestAlgorithmIsMissing() {
      Pseudonymizer pseudonymizer = new Pseudonymizer("salt");
      MockedStatic<MessageDigest> digest = Mockito.mockStatic(MessageDigest.class);

      try {
         digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new Throwable[]{new NoSuchAlgorithmException("none")});
         ((AbstractThrowableAssert)Assertions.assertThatThrownBy(() -> pseudonymizer.pseudonymize("value")).isInstanceOf(IllegalStateException.class)).hasMessageContaining("SHA-256");
      } catch (Throwable var6) {
         if (digest != null) {
            try {
               digest.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (digest != null) {
         digest.close();
      }

   }
}
