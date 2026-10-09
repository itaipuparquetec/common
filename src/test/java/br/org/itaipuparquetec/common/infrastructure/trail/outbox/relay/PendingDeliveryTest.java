package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.errors.TimeoutException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PendingDeliveryTest {
   private static final AuditOutboxRow ROW = new AuditOutboxRow(UUID.randomUUID(), "1", "acme_tenant", "mirror", "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", "{}", 0);

   PendingDeliveryTest() {
   }

   @AfterEach
   void clearInterruption() {
      Thread.interrupted();
   }

   @Test
   void shouldBeAcknowledgedWhenTheBrokerAnswersInTime() {
      PendingDelivery delivery = new PendingDelivery(ROW, CompletableFuture.completedFuture("ack"));
      Assertions.assertThat(delivery.acknowledgedBefore(Instant.now().plusSeconds(1L))).isTrue();
      Assertions.assertThat(delivery.brokerUnreachable()).isFalse();
   }

   @Test
   void shouldNotBeAcknowledgedWhenTheBrokerRejectsTheRecordButStillBeReachable() {
      PendingDelivery delivery = new PendingDelivery(ROW, CompletableFuture.failedFuture(new IllegalStateException("no")));
      Assertions.assertThat(delivery.acknowledgedBefore(Instant.now().plusSeconds(1L))).isFalse();
      Assertions.assertThat(delivery.brokerUnreachable()).isFalse();
   }

   @Test
   void shouldReportTheBrokerUnreachableWhenTheProducerTimedOutOnTheClusterMetadata() {
      PendingDelivery delivery = new PendingDelivery(ROW, CompletableFuture.failedFuture(new TimeoutException("metadata")));
      Assertions.assertThat(delivery.brokerUnreachable()).isTrue();
   }

   @Test
   void shouldNotBeAcknowledgedWhenTheDeadlineHasPassedAndTheBrokerDidNotAnswer() {
      PendingDelivery delivery = new PendingDelivery(ROW, new CompletableFuture());
      Assertions.assertThat(delivery.acknowledgedBefore(Instant.now().minusSeconds(1L))).isFalse();
      Assertions.assertThat(delivery.brokerUnreachable()).isFalse();
   }

   @Test
   void shouldNotBeAcknowledgedAndKeepTheInterruptionWhenTheThreadIsInterruptedWhileWaiting() {
      PendingDelivery delivery = new PendingDelivery(ROW, new InterruptedFuture());
      boolean acknowledged = delivery.acknowledgedBefore(Instant.now().plus(Duration.ofSeconds(1L)));
      Assertions.assertThat(acknowledged).isFalse();
      Assertions.assertThat(Thread.currentThread().isInterrupted()).isTrue();
   }

   private static final class InterruptedFuture implements Future<String> {
      private InterruptedFuture() {
      }

      public boolean cancel(boolean mayInterruptIfRunning) {
         return false;
      }

      public boolean isCancelled() {
         return false;
      }

      public boolean isDone() {
         return false;
      }

      public String get() throws InterruptedException {
         throw new InterruptedException("interrupted");
      }

      public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
         throw new InterruptedException("interrupted");
      }
   }
}
