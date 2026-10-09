package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.concurrent.RejectedExecutionException;

/**
 * Runs the writes of the events that must wait for the end of the business transaction, away from the request
 * thread. A write runs only after the request thread released its connection, so it never competes with it for the
 * pool of the tenant.
 */
public interface DeferredWrites {

    /**
     * @param write the work to run later
     * @throws RejectedExecutionException when too many writes are already waiting
     */
    void submit(Runnable write);
}
