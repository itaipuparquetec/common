package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * Runs the relay periodically on a scheduler of its own, so enabling the relay never turns on scheduling for the
 * rest of the application. A failing cycle never stops the following ones.
 */
@Slf4j
public class AuditOutboxRelayLifecycle implements SmartLifecycle {

    private final TaskScheduler scheduler;
    private final Runnable relayCycle;
    private final Duration interval;
    private ScheduledFuture<?> scheduled;

    public AuditOutboxRelayLifecycle(final TaskScheduler scheduler, final Runnable relayCycle,
                                     final Duration interval) {
        this.scheduler = scheduler;
        this.relayCycle = relayCycle;
        this.interval = interval;
    }

    @Override
    public synchronized void start() {
        scheduled = scheduler.scheduleWithFixedDelay(this::runCycleSafely, interval);
    }

    @Override
    public synchronized void stop() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return scheduled != null;
    }

    private void runCycleSafely() {
        try {
            relayCycle.run();
        } catch (final RuntimeException failure) {
            log.error("Audit outbox relay cycle failed", failure);
        }
    }
}
