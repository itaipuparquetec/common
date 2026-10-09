package br.org.itaipuparquetec.common.infrastructure.trail.sink;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditEventFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditPropertiesFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.RecordingAuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.envelope.TrailEventEnvelopeFactory;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantScope;
import br.org.itaipuparquetec.common.infrastructure.trail.metrics.AuditMetrics;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxEntryFactory;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.AuditOutboxNewTransactionWriter;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.FakeAuditOutboxStore;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.FakeTransactionManager;
import br.org.itaipuparquetec.common.infrastructure.trail.outbox.ImmediateDeferredWrites;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.AuditMappers;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.Pseudonymizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class OutboxAuditEventSinkTest {
   private final FakeAuditOutboxStore store = new FakeAuditOutboxStore();
   private final FakeTransactionManager transactions = new FakeTransactionManager();
   private final RecordingAuditEventSink fallback = new RecordingAuditEventSink();
   private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

   OutboxAuditEventSinkTest() {
   }

   @AfterEach
   void cleanTransactionState() {
      TransactionSynchronizationManager.clear();
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
      TransactionSynchronizationManager.setActualTransactionActive(false);
   }

   @Test
   void shouldJoinTheBusinessTransactionWhenTheWriteSucceeds() {
      beginBusinessTransaction(false);
      this.sinkWith(AuditMappers.envelope()).publish(AuditEventFixture.successOf("Register"));
      Assertions.assertThat(this.store.entries()).hasSize(1);
      Assertions.assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
      Assertions.assertThat(this.transactions.propagationsBegun()).isEmpty();
   }

   @Test
   void shouldFailTheBusinessOperationWhenTheEventOfASuccessCannotBeStoredInItsTransaction() {
      beginBusinessTransaction(false);
      this.store.failingWith(new IllegalStateException("outbox insert failed"));
      OutboxAuditEventSink sink = this.sinkWith(AuditMappers.envelope());
      AuditEvent event = AuditEventFixture.successOf("Register");
      ((AbstractThrowableAssert)Assertions.assertThatThrownBy(() -> sink.publish(event)).isInstanceOf(IllegalStateException.class)).hasMessage("outbox insert failed");
      Assertions.assertThat(this.fallback.events()).isEmpty();
   }

   @Test
   void shouldWaitForTheEndOfTheBusinessTransactionWhenTheUseCaseFailed() {
      beginBusinessTransaction(false);
      this.sinkWith(AuditMappers.envelope()).publish(AuditEventFixture.failureOf("Register"));
      Assertions.assertThat(this.store.entries()).isEmpty();
      completeBusinessTransaction(1);
      Assertions.assertThat(this.store.entries()).hasSize(1);
   }

   @Test
   void shouldWaitForTheEndOfTheBusinessTransactionWhenItIsReadOnly() {
      beginBusinessTransaction(true);
      this.sinkWith(AuditMappers.envelope()).publish(AuditEventFixture.successOf("Register"));
      Assertions.assertThat(this.store.entries()).isEmpty();
      completeBusinessTransaction(0);
      Assertions.assertThat(this.store.entries()).hasSize(1);
   }

   @Test
   void shouldWriteImmediatelyInANewTransactionWhenThereIsNoTransaction() {
      this.sinkWith(AuditMappers.envelope()).publish(AuditEventFixture.successOf("Register"));
      Assertions.assertThat(this.store.entries()).hasSize(1);
      Assertions.assertThat(this.transactions.commits()).isEqualTo(1);
   }

   @Test
   void shouldWriteImmediatelyInANewTransactionWhenAFailureHappensWithoutTransaction() {
      this.sinkWith(AuditMappers.envelope()).publish(AuditEventFixture.failureOf("Register"));
      Assertions.assertThat(this.store.entries()).hasSize(1);
      Assertions.assertThat(this.transactions.commits()).isEqualTo(1);
   }

   @Test
   void shouldFallBackToTheLogWithoutStoringWhenTheEnvelopeCannotBeSerialized() {
      AuditEvent event = AuditEventFixture.successOf("Register");
      this.sinkWith(new UnserializableObjectMapper()).publish(event);
      Assertions.assertThat(this.store.entries()).isEmpty();
      Assertions.assertThat(this.fallback.onlyEvent()).isSameAs(event);
      Assertions.assertThat(this.registry.counter("audit_outbox_write_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)1.0F);
   }

   private OutboxAuditEventSink sinkWith(ObjectMapper envelopeMapper) {
      TrailEventEnvelopeFactory envelopes = new TrailEventEnvelopeFactory(AuditMappers.masking(AuditPropertiesFixture.withCleartext(true, true, false), new Pseudonymizer("salt")), 1000);
      AuditOutboxEntryFactory entries = new AuditOutboxEntryFactory(envelopes, envelopeMapper);
      AuditOutboxNewTransactionWriter writer = new AuditOutboxNewTransactionWriter(this.transactions, this.store, this.fallback, new AuditMetrics(this.registry, "mirror"), new ImmediateDeferredWrites(), AuditTenantScope.none());
      return new OutboxAuditEventSink(entries, this.store, writer);
   }

   private static void beginBusinessTransaction(boolean readOnly) {
      TransactionSynchronizationManager.initSynchronization();
      TransactionSynchronizationManager.setActualTransactionActive(true);
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly);
   }

   private static void completeBusinessTransaction(int status) {
      TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCompletion(status));
   }

   private static final class UnserializableObjectMapper extends ObjectMapper {
      private UnserializableObjectMapper() {
      }

      @Override
      public String writeValueAsString(Object value) throws JsonProcessingException {
         throw new JsonProcessingException("cannot serialize") {
         };
      }
   }
}
