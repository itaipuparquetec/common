package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class KafkaAuditOutboxPublisherTest {
   private static final UUID EVENT_ID = UUID.fromString("01929f3c-7a1e-7c2b-9d4e-2f6a8b0c1d23");
   private final ScriptedProducer producer = new ScriptedProducer();
   private final KafkaAuditOutboxPublisher publisher;

   KafkaAuditOutboxPublisherTest() {
      this.publisher = new KafkaAuditOutboxPublisher(this.producer, "hubti.trail.events");
   }

   @Test
   void shouldSendThePayloadAsIsKeyedByTheTraceIdToTheTrailTopic() throws Exception {
      Future<?> future = this.publisher.publish(row());
      future.get();
      ProducerRecord<String, String> producedRecord = (ProducerRecord)this.producer.history().get(0);
      Assertions.assertThat(producedRecord.topic()).isEqualTo("hubti.trail.events");
      Assertions.assertThat((String)producedRecord.key()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
      Assertions.assertThat((String)producedRecord.value()).isEqualTo("{\"schemaVersion\":\"1\"}");
   }

   @Test
   void shouldSendTheHeadersOfTheContractAndNoSpringTypeHeader() {
      this.publisher.publish(row());
      Headers headers = ((ProducerRecord)this.producer.history().get(0)).headers();
      Assertions.assertThat(headerValue(headers.lastHeader("trail-schema-version").value())).isEqualTo("1");
      Assertions.assertThat(headerValue(headers.lastHeader("trail-event-id").value())).isEqualTo(EVENT_ID.toString());
      Assertions.assertThat(headerValue(headers.lastHeader("trail-tenant").value())).isEqualTo("acme_tenant");
      Assertions.assertThat(headerValue(headers.lastHeader("trail-source-service").value())).isEqualTo("mirror");
      Assertions.assertThat(headerValue(headers.lastHeader("content-type").value())).isEqualTo("application/json");
      Assertions.assertThat(headerValue(headers.lastHeader("traceparent").value())).isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
      Assertions.assertThat(headers.lastHeader("__TypeId__")).isNull();
   }

   @Test
   void shouldReturnAFailedFutureWhenTheProducerCannotTakeTheRecord() {
      this.producer.failingToSend(EVENT_ID);
      Future<?> future = this.publisher.publish(row());
      Assertions.assertThat(future).isDone();
      Objects.requireNonNull(future);
      ((AbstractThrowableAssert)Assertions.assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)).hasMessageContaining("cannot hand over");
   }

   @Test
   void shouldCloseTheProducerWhenClosed() {
      this.publisher.close();
      Assertions.assertThat(this.producer.closed()).isTrue();
   }

   private static AuditOutboxRow row() {
      return new AuditOutboxRow(EVENT_ID, "1", "acme_tenant", "mirror", "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", "{\"schemaVersion\":\"1\"}", 0);
   }

   private static String headerValue(byte[] value) {
      return new String(value, StandardCharsets.UTF_8);
   }
}
