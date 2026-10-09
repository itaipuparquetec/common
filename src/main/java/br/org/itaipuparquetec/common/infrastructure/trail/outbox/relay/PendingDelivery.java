package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import org.apache.kafka.common.errors.TimeoutException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An outbox row handed to the producer, waiting for the broker acknowledgement.
 */
record PendingDelivery(AuditOutboxRow row, Future<?> acknowledgement) {

    /**
     * The producer gave up reaching the cluster (metadata timeout): sending the rest of the batch would only repeat
     * the wait for every row.
     */
    boolean brokerUnreachable() {
        return acknowledgement.state() == Future.State.FAILED
                && acknowledgement.exceptionNow() instanceof TimeoutException;
    }

    boolean acknowledgedBefore(final Instant deadline) {
        final var remaining = Duration.between(Instant.now(), deadline);
        try {
            acknowledgement.get(Math.max(remaining.toNanos(), 0), TimeUnit.NANOSECONDS);
            return true;
        } catch (final ExecutionException | java.util.concurrent.TimeoutException failure) {
            return false;
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
