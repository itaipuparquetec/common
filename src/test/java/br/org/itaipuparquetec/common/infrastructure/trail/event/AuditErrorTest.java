package br.org.itaipuparquetec.common.infrastructure.trail.event;

import br.org.itaipuparquetec.common.domain.exceptions.DomainException;
import br.org.itaipuparquetec.common.domain.exceptions.TechnicalException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AuditErrorTest {
   AuditErrorTest() {
   }

   @Test
   void shouldBuildATechnicalErrorFromATechnicalException() {
      AuditError error = AuditError.of(new TechnicalException("db password=secret"));
      Assertions.assertThat(error).isEqualTo(new AuditError("TechnicalException", AuditErrorCategory.TECHNICAL));
   }

   @Test
   void shouldBuildATechnicalErrorFromAnUnexpectedException() {
      AuditError error = AuditError.of(new IllegalStateException("boom"));
      Assertions.assertThat(error).isEqualTo(new AuditError("TechnicalException", AuditErrorCategory.TECHNICAL));
   }

   @Test
   void shouldBuildABusinessErrorWithTheSimpleNameButNeverTheMessageOfTheException() {
      AuditError error = AuditError.of(new InvalidOrderException("invalid order of john@doe.com"));
      Assertions.assertThat(error).isEqualTo(new AuditError("InvalidOrderException", AuditErrorCategory.BUSINESS));
      Assertions.assertThat(error.toString()).doesNotContain(new CharSequence[]{"john@doe.com"});
   }

   @Test
   void shouldMapEachCategoryToItsAuditResult() {
      Assertions.assertThat(AuditErrorCategory.BUSINESS.result()).isEqualTo(AuditResult.BUSINESS_ERROR);
      Assertions.assertThat(AuditErrorCategory.TECHNICAL.result()).isEqualTo(AuditResult.TECHNICAL_ERROR);
   }

   private static final class InvalidOrderException extends RuntimeException implements DomainException {
      private InvalidOrderException(String message) {
         super(message);
      }
   }
}
