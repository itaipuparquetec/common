package br.org.itaipuparquetec.common.infrastructure.audit.trail;

import br.org.itaipuparquetec.common.infrastructure.audit.trail.context.AuditContext;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.context.TokenClaimsReader;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.event.AuditError;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.event.AuditResult;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.domain.exceptions.DomainException;
import br.org.itaipuparquetec.common.domain.exceptions.TechnicalException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Intercepts every {@code UseCase} execution and builds a structured audit event: who did what, when, with
 * which data and with which result. Context identifiers are kept in the MDC; input and output bodies are passed
 * directly to the event, never through the MDC.
 */
@Aspect
public class AuditTrailAspect {

    private static final String DEFAULT_VERSION = "1";
    private static final String IMPL_SUFFIX = "Impl";
    private static final String SUB_CLAIM = "sub";

    private final AuditEventSink sink;
    private final TokenClaimsReader claims;

    public AuditTrailAspect(final AuditEventSink sink, final TokenClaimsReader claims) {
        this.sink = sink;
        this.claims = claims;
    }

    @Around("execution(* br.org.itaipuparquetec.common.application.usecases.UseCase+.execute(..)) "
            + "|| execution(* br.org.itaipuparquetec.common.application.usecases.UnitUseCase+.execute(..)) "
            + "|| execution(* br.org.itaipuparquetec.common.application.usecases.NullaryUseCase+.execute(..))")
    public Object audit(final ProceedingJoinPoint joinPoint) throws Throwable {
        final var identity = resolveIdentity(joinPoint);
        final var input = firstArgumentOf(joinPoint);
        AuditContext.put(identity.actor(), identity.sid(), identity.jti(), identity.traceId());
        final var startedAt = Instant.now();
        final var startNanos = System.nanoTime();
        try {
            final var output = joinPoint.proceed();
            sink.publish(event(identity, startedAt, startNanos, AuditResult.SUCCESS, input, output, null));
            return output;
        } catch (final Throwable failure) {
            final var classified = classify(failure);
            sink.publish(event(identity, startedAt, startNanos, classified.result(), input, null, classified.error()));
            throw failure;
        } finally {
            AuditContext.clear();
        }
    }

    private ExecutionIdentity resolveIdentity(final ProceedingJoinPoint joinPoint) {
        final var target = joinPoint.getTarget().getClass();
        return new ExecutionIdentity(
                resolveUseCaseName(target),
                resolveVersion(target),
                claims.claim(SUB_CLAIM).orElse(null),
                claims.claim(AuditContext.SID).orElse(null),
                claims.claim(AuditContext.JTI).orElse(null),
                resolveTraceId());
    }

    private static AuditEvent event(final ExecutionIdentity identity, final Instant startedAt, final long startNanos,
                                    final AuditResult result, final Object input, final Object output,
                                    final AuditError error) {
        final var duration = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        return new AuditEvent(identity.useCase(), identity.version(), identity.actor(), identity.sid(),
                identity.jti(), identity.traceId(), startedAt, duration, result, input, output, error);
    }

    private static AuditFailure classify(final Throwable failure) {
        if (failure instanceof TechnicalException) {
            return new AuditFailure(AuditResult.TECHNICAL_ERROR, AuditError.technical());
        }
        if (failure instanceof DomainException domain) {
            return new AuditFailure(AuditResult.BUSINESS_ERROR, AuditError.business(domain));
        }
        return new AuditFailure(AuditResult.TECHNICAL_ERROR, AuditError.technical());
    }

    private static String resolveUseCaseName(final Class<?> target) {
        final var name = target.getSimpleName();
        return name.endsWith(IMPL_SUFFIX) ? name.substring(0, name.length() - IMPL_SUFFIX.length()) : name;
    }

    private static String resolveVersion(final Class<?> target) {
        final var version = target.getAnnotation(AuditVersion.class);
        return version == null ? DEFAULT_VERSION : version.value();
    }

    private static String resolveTraceId() {
        final var current = MDC.get(AuditContext.TRACE_ID);
        return current == null ? UUID.randomUUID().toString() : current;
    }

    private static Object firstArgumentOf(final ProceedingJoinPoint joinPoint) {
        final var arguments = joinPoint.getArgs();
        return arguments.length > 0 ? arguments[0] : null;
    }

    private record ExecutionIdentity(String useCase, String version, String actor, String sid, String jti,
                                     String traceId) {
    }

    private record AuditFailure(AuditResult result, AuditError error) {
    }
}
