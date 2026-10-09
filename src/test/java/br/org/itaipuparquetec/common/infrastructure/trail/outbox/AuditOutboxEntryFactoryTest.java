package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditEventFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditPropertiesFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.envelope.TrailEventEnvelopeFactory;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.AuditMappers;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.Pseudonymizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AuditOutboxEntryFactoryTest {
   private final TrailEventEnvelopeFactory envelopes = new TrailEventEnvelopeFactory(AuditMappers.masking(AuditPropertiesFixture.withCleartext(true, true, false), new Pseudonymizer("salt")), 1000);

   AuditOutboxEntryFactoryTest() {
   }

   @Test
   void shouldCopyTheKeysNeededToPublishAndSerializeTheEnvelopeAsThePayload() {
      AuditOutboxEntryFactory factory = new AuditOutboxEntryFactory(this.envelopes, AuditMappers.envelope());
      AuditOutboxEntry entry = factory.entryOf(AuditEventFixture.successWithInput("Register", "order-7"));
      Assertions.assertThat(entry.eventId()).isEqualTo(AuditEventFixture.EVENT_ID);
      Assertions.assertThat(entry.schemaVersion()).isEqualTo("1");
      Assertions.assertThat(entry.tenant()).isEqualTo("acme_tenant");
      Assertions.assertThat(entry.sourceService()).isEqualTo("mirror");
      Assertions.assertThat(entry.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
      Assertions.assertThat(entry.spanId()).isEqualTo("00f067aa0ba902b7");
      Assertions.assertThat(entry.payload()).contains(new CharSequence[]{"\"eventId\":\"" + String.valueOf(AuditEventFixture.EVENT_ID) + "\"", "\"input\":\"\\\"order-7\\\"\"", "\"result\":\"SUCCESS\""});
   }

   @Test
   void shouldNeverPutTheOutputOrAnErrorMessageInThePayload() {
      AuditOutboxEntryFactory factory = new AuditOutboxEntryFactory(this.envelopes, AuditMappers.envelope());
      AuditOutboxEntry entry = factory.entryOf(AuditEventFixture.failureOf("Register"));
      ((AbstractStringAssert)Assertions.assertThat(entry.payload()).contains(new CharSequence[]{"\"error\":{\"type\":\"ItemAlreadyExistsException\",\"category\":\"BUSINESS\"}"})).doesNotContain(new CharSequence[]{"output", "message"});
   }

   @Test
   void shouldFailWithTheEventIdWhenTheEnvelopeCannotBeSerialized() {
      AuditOutboxEntryFactory factory = new AuditOutboxEntryFactory(this.envelopes, new UnserializableObjectMapper());
      AuditEvent event = AuditEventFixture.successOf("Register");
      ((AbstractThrowableAssert)Assertions.assertThatThrownBy(() -> factory.entryOf(event)).isInstanceOf(AuditEnvelopeSerializationException.class)).hasMessageContaining(AuditEventFixture.EVENT_ID.toString());
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
