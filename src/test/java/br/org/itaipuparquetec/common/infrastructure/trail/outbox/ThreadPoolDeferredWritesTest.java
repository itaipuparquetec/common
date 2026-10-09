package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ThreadPoolDeferredWritesTest {
   ThreadPoolDeferredWritesTest() {
   }

   @Test
   void shouldRunTheWriteOnADaemonWriterThreadOtherThanTheCaller() throws Exception {
      ThreadPoolDeferredWrites writes = new ThreadPoolDeferredWrites(1, 10);
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<Thread> thread = new AtomicReference();
      writes.submit(() -> {
         thread.set(Thread.currentThread());
         done.countDown();
      });
      Assertions.assertThat(done.await(5L, TimeUnit.SECONDS)).isTrue();
      Assertions.assertThat((Thread)thread.get()).isNotSameAs(Thread.currentThread());
      Assertions.assertThat(((Thread)thread.get()).getName()).startsWith("audit-outbox-writer-");
      Assertions.assertThat(((Thread)thread.get()).isDaemon()).isTrue();
      writes.close();
   }

   @Test
   void shouldRejectTheWriteWhenThePoolAndTheQueueAreFull() throws Exception {
      ThreadPoolDeferredWrites writes = new ThreadPoolDeferredWrites(1, 1);
      CountDownLatch gate = new CountDownLatch(1);
      CountDownLatch started = new CountDownLatch(1);
      writes.submit(() -> {
         started.countDown();
         awaitQuietly(gate);
      });
      Assertions.assertThat(started.await(5L, TimeUnit.SECONDS)).isTrue();
      writes.submit(() -> {
      });
      Assertions.assertThatThrownBy(() -> writes.submit(() -> {
         })).isInstanceOf(RejectedExecutionException.class);
      gate.countDown();
      writes.close();
   }

   @Test
   void shouldDrainThePendingWritesWhenClosed() throws Exception {
      ThreadPoolDeferredWrites writes = new ThreadPoolDeferredWrites(1, 10);
      AtomicReference<Integer> executed = new AtomicReference(0);

      for(int number = 0; number < 5; ++number) {
         writes.submit(() -> executed.updateAndGet(count -> count + 1));
      }

      writes.close();
      Assertions.assertThat((Integer)executed.get()).isEqualTo(5);
   }

   private static void awaitQuietly(CountDownLatch gate) {
      try {
         gate.await(5L, TimeUnit.SECONDS);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }

   }
}
