package br.org.itaipuparquetec.common.infrastructure.trail.envelope;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditEventFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditPropertiesFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Public;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Sensitive;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.AuditMappers;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.Pseudonymizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TrailEventEnvelopeFactoryTest {
   private final ObjectMapper maskingMapper = AuditMappers.masking(AuditPropertiesFixture.withCleartext(true, true, false), new Pseudonymizer("salt"));
   private final TrailEventEnvelopeFactory factory;

   TrailEventEnvelopeFactoryTest() {
      this.factory = new TrailEventEnvelopeFactory(this.maskingMapper, 200);
   }

   @Test
   void shouldMapTheIdentityTraceAndSourceOfTheEvent() {
      AuditEvent event = AuditEventFixture.successOf("InsertNewItemUseCase");
      TrailEventEnvelope envelope = this.factory.envelopeOf(event);
      Assertions.assertThat(envelope.schemaVersion()).isEqualTo("1");
      Assertions.assertThat(envelope.eventId()).isEqualTo(AuditEventFixture.EVENT_ID.toString());
      Assertions.assertThat(envelope.occurredAt()).isEqualTo("2026-10-07T13:45:12.345Z");
      Assertions.assertThat(envelope.durationMillis()).isEqualTo(42L);
      Assertions.assertThat(envelope.tenant()).isEqualTo("acme_tenant");
      Assertions.assertThat(envelope.actorTenant()).isEqualTo("acme");
      Assertions.assertThat(envelope.sourceService()).isEqualTo("mirror");
      Assertions.assertThat(envelope.sourceVersion()).isEqualTo("0.8.0");
      Assertions.assertThat(envelope.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
      Assertions.assertThat(envelope.spanId()).isEqualTo("00f067aa0ba902b7");
      Assertions.assertThat(envelope.parentSpanId()).isEqualTo("a3ce929d0e0e4736");
      Assertions.assertThat(envelope.useCase()).isEqualTo("InsertNewItemUseCase");
      Assertions.assertThat(envelope.useCaseVersion()).isEqualTo("1");
      Assertions.assertThat(envelope.actor()).isEqualTo("user-1");
      Assertions.assertThat(envelope.actorType()).isEqualTo("USER");
      Assertions.assertThat(envelope.sid()).isEqualTo("sid-1");
      Assertions.assertThat(envelope.jti()).isEqualTo("jti-1");
      Assertions.assertThat(envelope.result()).isEqualTo("SUCCESS");
      Assertions.assertThat(envelope.error()).isNull();
      Assertions.assertThat(envelope.input()).isNull();
      Assertions.assertThat(envelope.inputOmitted()).isNull();
   }

   @Test
   void shouldFormatTheOccurrenceInUtcWithMilliseconds() {
      AuditEvent event = AuditEventFixture.successOf("Register");
      TrailEventEnvelope envelope = this.factory.envelopeOf(event);
      Assertions.assertThat(envelope.occurredAt()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
   }

   @Test
   void shouldMapTheErrorWithTypeAndCategoryOnly() {
      AuditEvent event = AuditEventFixture.failureOf("Register");
      TrailEventEnvelope envelope = this.factory.envelopeOf(event);
      Assertions.assertThat(envelope.result()).isEqualTo("BUSINESS_ERROR");
      Assertions.assertThat(envelope.error()).isEqualTo(new TrailEventEnvelope.Error("ItemAlreadyExistsException", "BUSINESS"));
   }

   @Test
   void shouldCarryTheInputAsAMaskedJsonDocumentInAString() {
      AuditEvent event = AuditEventFixture.successWithInput("Register", new Customer("Servidor A", "123.456.789-00"));
      TrailEventEnvelope envelope = this.factory.envelopeOf(event);
      ((AbstractStringAssert)Assertions.assertThat(envelope.input()).contains(new CharSequence[]{"\"name\":\"Servidor A\""})).doesNotContain(new CharSequence[]{"123.456.789-00"});
      Assertions.assertThat(envelope.inputOmitted()).isNull();
   }

   @Test
   void shouldOmitTheInputAboveTheSizeLimitInsteadOfTruncatingIt() {
      AuditEvent event = AuditEventFixture.successWithInput("Register", new Customer("x".repeat(500), "doc"));
      TrailEventEnvelope envelope = this.factory.envelopeOf(event);
      Assertions.assertThat(envelope.input()).isNull();
      Assertions.assertThat(envelope.inputOmitted()).isEqualTo("SIZE_LIMIT");
   }

   @Test
   void shouldMeasureTheSizeLimitInUtf8Bytes() {
      TrailEventEnvelopeFactory limited = new TrailEventEnvelopeFactory(this.maskingMapper, 11);
      AuditEvent event = AuditEventFixture.successWithInput("Register", "ççççç");
      TrailEventEnvelope envelope = limited.envelopeOf(event);
      Assertions.assertThat(envelope.inputOmitted()).isEqualTo("SIZE_LIMIT");
   }

   @Test
   void shouldOmitTheInputWithAReasonWhenItCannotBeSerialized() {
      TrailEventEnvelopeFactory failing = new TrailEventEnvelopeFactory(new UnserializableObjectMapper(), 200);
      AuditEvent event = AuditEventFixture.successWithInput("Register", "any");
      TrailEventEnvelope envelope = failing.envelopeOf(event);
      Assertions.assertThat(envelope.input()).isNull();
      Assertions.assertThat(envelope.inputOmitted()).isEqualTo("SERIALIZATION_ERROR");
   }

   static record Customer(@Public String name, @Sensitive String document) {
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
