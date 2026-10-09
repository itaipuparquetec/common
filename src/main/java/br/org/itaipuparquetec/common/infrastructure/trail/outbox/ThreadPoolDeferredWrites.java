package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link DeferredWrites} backed by a small bounded pool of daemon threads. On close it drains the pending writes
 * before the connection pools go away.
 */
public class ThreadPoolDeferredWrites implements DeferredWrites, AutoCloseable {

    private static final long DRAIN_TIMEOUT_SECONDS = 30;

    private final ThreadPoolExecutor executor;

    public ThreadPoolDeferredWrites(final int threads, final int queueCapacity) {
        this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), new WriterThreads(), new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void submit(final Runnable write) {
        executor.execute(write);
    }

    @Override
    public void close() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static final class WriterThreads implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(final Runnable task) {
            final var thread = new Thread(task, "audit-outbox-writer-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
