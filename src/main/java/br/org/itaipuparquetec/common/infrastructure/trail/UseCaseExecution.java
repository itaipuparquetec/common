package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditActor;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.TraceSpan;

import java.time.Duration;
import java.time.Instant;

/**
 * What is known about a use case execution from its start: who ran it, in which span, with which input.
 */
record UseCaseExecution(AuditActor actor, TraceSpan span, String useCase, String useCaseVersion, Object input,
                        Instant startedAt, long startNanos) {

    private static final String DEFAULT_VERSION = "1";
    private static final String IMPL_SUFFIX = "Impl";

    static UseCaseExecution begin(final AuditActor actor, final TraceSpan span, final Class<?> useCaseType,
                                  final Object input) {
        return new UseCaseExecution(actor, span, useCaseNameOf(useCaseType), versionOf(useCaseType), input,
                Instant.now(), System.nanoTime());
    }

    static String useCaseNameOf(final Class<?> useCaseType) {
        final var name = useCaseType.getSimpleName();
        return name.endsWith(IMPL_SUFFIX) ? name.substring(0, name.length() - IMPL_SUFFIX.length()) : name;
    }

    long elapsedMillis() {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private static String versionOf(final Class<?> useCaseType) {
        final var version = useCaseType.getAnnotation(AuditVersion.class);
        return version == null ? DEFAULT_VERSION : version.value();
    }
}
