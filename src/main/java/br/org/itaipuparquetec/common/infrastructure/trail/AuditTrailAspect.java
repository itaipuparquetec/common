package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.context.AuditContext;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditActorResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.UseCaseTracing;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Intercepts every {@code UseCase} execution and builds a structured audit event: who did what, when, in which
 * step of the request, and with which result. Each execution opens a child span of the span in effect (W3C Trace
 * Context, ADR-032) and an audit context in the MDC that is restored when the execution ends, so nested use cases
 * never erase the context of the one that called them. Input bodies are passed directly to the event, never through
 * the MDC; the output is never recorded.
 *
 * <p>Read-only executions are skipped unless included by configuration or marked with
 * {@link AuditSensitiveRead}. A failure to deliver the event of a successful execution propagates to the caller
 * (the business operation must not complete without its trail); the event of a failed execution never masks the
 * original failure.</p>
 */
@Aspect
public class AuditTrailAspect {

    private final AuditEventSink sink;
    private final AuditActorResolver actors;
    private final UseCaseTracing tracing;
    private final AuditEventAssembler assembler;
    private final AuditPolicy policy;

    public AuditTrailAspect(final AuditEventSink sink, final AuditActorResolver actors, final UseCaseTracing tracing,
                            final AuditEventAssembler assembler, final AuditPolicy policy) {
        this.sink = sink;
        this.actors = actors;
        this.tracing = tracing;
        this.assembler = assembler;
        this.policy = policy;
    }

    @Around("execution(* br.org.itaipuparquetec.common.application.usecases.UseCase+.execute(..)) "
            + "|| execution(* br.org.itaipuparquetec.common.application.usecases.UnitUseCase+.execute(..)) "
            + "|| execution(* br.org.itaipuparquetec.common.application.usecases.NullaryUseCase+.execute(..))")
    public Object audit(final ProceedingJoinPoint joinPoint) throws Throwable {
        final var useCaseType = joinPoint.getTarget().getClass();
        if (policy.skips(useCaseType)) {
            return joinPoint.proceed();
        }
        final var actor = actors.current();
        try (var span = tracing.open(UseCaseExecution.useCaseNameOf(useCaseType));
             var ignored = AuditContext.open(actor.subject(), actor.sid(), actor.jti(), span.traceId(), span.spanId())) {
            final var execution = UseCaseExecution.begin(actor, span, useCaseType, firstArgumentOf(joinPoint));
            return proceedAndPublish(joinPoint, execution);
        }
    }

    private Object proceedAndPublish(final ProceedingJoinPoint joinPoint, final UseCaseExecution execution)
            throws Throwable {
        final Object output;
        try {
            output = joinPoint.proceed();
        } catch (final Throwable failure) {
            publishFailure(execution, failure);
            throw failure;
        }
        sink.publish(assembler.succeeded(execution));
        return output;
    }

    private void publishFailure(final UseCaseExecution execution, final Throwable failure) {
        try {
            sink.publish(assembler.failed(execution, failure));
        } catch (final RuntimeException publishing) {
            failure.addSuppressed(publishing);
        }
    }

    private static Object firstArgumentOf(final ProceedingJoinPoint joinPoint) {
        final var arguments = joinPoint.getArgs();
        return arguments.length > 0 ? arguments[0] : null;
    }
}
