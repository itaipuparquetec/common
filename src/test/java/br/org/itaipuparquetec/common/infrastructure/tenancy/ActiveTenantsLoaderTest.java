package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ActiveTenantsLoaderTest {

    private final InMemoryActiveTenantDirectory directory = new InMemoryActiveTenantDirectory();
    private final ScheduledExecutorService retryScheduler = mock(ScheduledExecutorService.class);

    @Test
    void loadsTheActiveTenantsWhenTheApplicationIsReady() {
        final var loader = new ActiveTenantsLoader(tenants("acme_tenant", "beta_tenant"), directory, retryScheduler);

        loader.onApplicationEvent(mock(ApplicationReadyEvent.class));

        assertThat(directory.isActive("acme_tenant")).isTrue();
        assertThat(directory.isActive("beta_tenant")).isTrue();
        verify(retryScheduler).shutdown();
        verify(retryScheduler, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    void triesAgainLaterWhileTheSourceIsUnreachable() {
        final var attempts = new AtomicInteger();
        final ActiveTenantsSource unreachableOnce = consumer -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("TenantService unreachable");
            }
            consumer.accept("acme_tenant");
        };
        final var loader = new ActiveTenantsLoader(unreachableOnce, directory, retryScheduler);

        loader.loadOrRetryLater();

        assertThat(directory.isActive("acme_tenant")).isFalse();
        final var retry = ArgumentCaptor.forClass(Runnable.class);
        verify(retryScheduler).schedule(retry.capture(), eq(30L), eq(TimeUnit.SECONDS));
        retry.getValue().run();
        assertThat(directory.isActive("acme_tenant")).isTrue();
    }

    private static ActiveTenantsSource tenants(final String... names) {
        return (final Consumer<String> consumer) -> {
            for (final var name : names) {
                consumer.accept(name);
            }
        };
    }
}
