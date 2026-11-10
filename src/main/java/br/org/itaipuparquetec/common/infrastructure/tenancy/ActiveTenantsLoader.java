package br.org.itaipuparquetec.common.infrastructure.tenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fills the {@link InMemoryActiveTenantDirectory} once, when the application is ready (its own issuer may be needed
 * to get the token of the load). While the source is unreachable, it tries again in the background: meanwhile only
 * the central tenant and the tenants of the events received are accepted.
 */
@Slf4j
public class ActiveTenantsLoader implements ApplicationListener<ApplicationReadyEvent> {

    static final Duration RETRY_AFTER = Duration.ofSeconds(30);

    private final ActiveTenantsSource activeTenantsSource;
    private final InMemoryActiveTenantDirectory directory;
    private final ScheduledExecutorService retryScheduler;

    public ActiveTenantsLoader(final ActiveTenantsSource activeTenantsSource, final InMemoryActiveTenantDirectory directory,
                               final ScheduledExecutorService retryScheduler) {
        this.activeTenantsSource = activeTenantsSource;
        this.directory = directory;
        this.retryScheduler = retryScheduler;
    }

    @Override
    public void onApplicationEvent(@NonNull final ApplicationReadyEvent event) {
        loadOrRetryLater();
    }

    void loadOrRetryLater() {
        try {
            activeTenantsSource.forEachActiveTenant(directory::activate);
            retryScheduler.shutdown();
        } catch (final RuntimeException exception) {
            log.warn("Could not load the active tenants; trying again in {} seconds", RETRY_AFTER.toSeconds(), exception);
            retryScheduler.schedule(this::loadOrRetryLater, RETRY_AFTER.toSeconds(), TimeUnit.SECONDS);
        }
    }
}
