package br.org.itaipuparquetec.common.infrastructure.trail;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AuditPolicyTest {
   AuditPolicyTest() {
   }

   @AfterEach
   void cleanTransactionState() {
      TransactionSynchronizationManager.clear();
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
   }

   @ParameterizedTest
   @CsvSource({"false, false, false, false", "false, true, false, true", "true, true, false, false", "false, true, true, false", "true, true, true, false"})
   void shouldSkipOnlyUnmarkedUseCasesInAReadOnlyTransactionWhenReadOnlyIsNotIncluded(boolean includeReadOnly, boolean readOnlyTransaction, boolean markedSensitive, boolean expectedSkip) {
      enterTransaction(readOnlyTransaction);
      AuditPolicy policy = new AuditPolicy(includeReadOnly);
      Class<? extends Object> type = markedSensitive ? SensitiveReadUseCase.class : PlainUseCase.class;
      boolean skips = policy.skips(type);
      Assertions.assertThat(skips).isEqualTo(expectedSkip);
   }

   @ParameterizedTest
   @CsvSource({"true", "false"})
   void shouldNeverSkipAWritingUseCaseOutsideAReadOnlyTransaction(boolean includeReadOnly) {
      AuditPolicy policy = new AuditPolicy(includeReadOnly);
      boolean skips = policy.skips(PlainUseCase.class);
      Assertions.assertThat(skips).isFalse();
   }

   private static void enterTransaction(boolean readOnly) {
      TransactionSynchronizationManager.initSynchronization();
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly);
   }

   static class PlainUseCase {
      PlainUseCase() {
      }
   }

   @AuditSensitiveRead
   static class SensitiveReadUseCase {
      SensitiveReadUseCase() {
      }
   }
}
