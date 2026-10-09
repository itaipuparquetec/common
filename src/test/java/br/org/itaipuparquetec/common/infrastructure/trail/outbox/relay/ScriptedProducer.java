package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringSerializer;

final class ScriptedProducer extends MockProducer<String, String> {
   private final Set<String> rejectedByBroker = new HashSet();
   private final Set<String> failingToSend = new HashSet();
   private final Set<String> unreachable = new HashSet();
   private final Set<String> neverAcknowledged = new HashSet();
   private final CountDownLatch sendAttempted = new CountDownLatch(1);
   private CountDownLatch blockingGate;

   ScriptedProducer() {
      super(true, new StringSerializer(), new StringSerializer());
   }

   ScriptedProducer rejecting(Object eventId) {
      this.rejectedByBroker.add(eventId.toString());
      return this;
   }

   ScriptedProducer failingToSend(Object eventId) {
      this.failingToSend.add(eventId.toString());
      return this;
   }

   ScriptedProducer unreachableFor(Object eventId) {
      this.unreachable.add(eventId.toString());
      return this;
   }

   ScriptedProducer neverAcknowledging(Object eventId) {
      this.neverAcknowledged.add(eventId.toString());
      return this;
   }

   ScriptedProducer blockingUntil(CountDownLatch gate) {
      this.blockingGate = gate;
      return this;
   }

   @Override
   public Future<RecordMetadata> send(ProducerRecord<String, String> producedRecord) {
      this.sendAttempted.countDown();
      this.awaitGate();
      String eventId = this.headerOf(producedRecord, "trail-event-id");
      if (this.failingToSend.contains(eventId)) {
         throw new KafkaException("cannot hand over " + eventId);
      } else if (this.unreachable.contains(eventId)) {
         return CompletableFuture.failedFuture(new TimeoutException("metadata not available for " + eventId));
      } else if (this.rejectedByBroker.contains(eventId)) {
         return CompletableFuture.failedFuture(new KafkaException("rejected " + eventId));
      } else {
         return (Future<RecordMetadata>)(this.neverAcknowledged.contains(eventId) ? new CompletableFuture() : super.send(producedRecord));
      }
   }

   boolean awaitSendAttempt(Duration timeout) throws InterruptedException {
      return this.sendAttempted.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
   }

   String headerOf(ProducerRecord<String, String> producedRecord, String name) {
      return new String(producedRecord.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
   }

   private void awaitGate() {
      if (this.blockingGate != null) {
         try {
            this.blockingGate.await();
         } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
         }

      }
   }
}
