package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class AuditOutboxRelayLifecycleTest {
   private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
   private final AtomicInteger cycles = new AtomicInteger();

   AuditOutboxRelayLifecycleTest() {
      this.scheduler.setPoolSize(1);
      this.scheduler.initialize();
   }

   @AfterEach
   void shutDownScheduler() {
      this.scheduler.shutdown();
   }

   @Test
   void shouldRunTheRelayCycleRepeatedlyAfterStart() {
      ThreadPoolTaskScheduler var10002 = this.scheduler;
      AtomicInteger var10003 = this.cycles;
      Objects.requireNonNull(var10003);
      AuditOutboxRelayLifecycle lifecycle = new AuditOutboxRelayLifecycle(var10002, var10003::incrementAndGet, Duration.ofMillis(10L));
      lifecycle.start();
      Awaitility.await().atMost(Duration.ofSeconds(5L)).until(() -> this.cycles.get() >= 3);
      Assertions.assertThat(lifecycle.isRunning()).isTrue();
      lifecycle.stop();
   }

   @Test
   void shouldStopRunningCyclesAfterStop() {
      ThreadPoolTaskScheduler var10002 = this.scheduler;
      AtomicInteger var10003 = this.cycles;
      Objects.requireNonNull(var10003);
      AuditOutboxRelayLifecycle lifecycle = new AuditOutboxRelayLifecycle(var10002, var10003::incrementAndGet, Duration.ofMillis(10L));
      lifecycle.start();
      Awaitility.await().atMost(Duration.ofSeconds(5L)).until(() -> this.cycles.get() >= 1);
      lifecycle.stop();
      int cyclesAfterStop = this.awaitStableCycleCount();
      Assertions.assertThat(lifecycle.isRunning()).isFalse();
      Assertions.assertThat(this.cycles.get()).isEqualTo(cyclesAfterStop);
   }

   private int awaitStableCycleCount() {
      AtomicInteger lastSeen = new AtomicInteger(-1);
      Awaitility.await()
         .atMost(Duration.ofSeconds(2L))
         .during(Duration.ofMillis(150L))
         .until(() -> {
            int current = this.cycles.get();
            boolean stable = current == lastSeen.get();
            lastSeen.set(current);
            return stable;
         });
      return this.cycles.get();
   }

   @Test
   void shouldKeepRunningTheNextCyclesWhenACycleFails() {
      Runnable failingOnce = () -> {
         if (this.cycles.incrementAndGet() == 1) {
            throw new IllegalStateException("database down");
         }
      };
      AuditOutboxRelayLifecycle lifecycle = new AuditOutboxRelayLifecycle(this.scheduler, failingOnce, Duration.ofMillis(10L));
      lifecycle.start();
      Awaitility.await().atMost(Duration.ofSeconds(5L)).until(() -> this.cycles.get() >= 3);
      lifecycle.stop();
   }

   @Test
   void shouldNotBeRunningBeforeStartAndAcceptAStopWithoutStart() {
      ThreadPoolTaskScheduler var10002 = this.scheduler;
      AtomicInteger var10003 = this.cycles;
      Objects.requireNonNull(var10003);
      AuditOutboxRelayLifecycle lifecycle = new AuditOutboxRelayLifecycle(var10002, var10003::incrementAndGet, Duration.ofMillis(10L));
      lifecycle.stop();
      Assertions.assertThat(lifecycle.isRunning()).isFalse();
   }
}
