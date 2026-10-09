package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.envelope.UuidV7Generator;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditError;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditResult;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.SourceIdentity;

/**
 * Builds the {@link AuditEvent} of a finished use case execution: a fresh UUIDv7 event id, the tenant in effect,
 * the producing service and the outcome.
 */
public class AuditEventAssembler {

    private final UuidV7Generator eventIds;
    private final SourceIdentity source;
    private final AuditTenantResolver tenants;

    public AuditEventAssembler(final UuidV7Generator eventIds, final SourceIdentity source,
                               final AuditTenantResolver tenants) {
        this.eventIds = eventIds;
        this.source = source;
        this.tenants = tenants;
    }

    AuditEvent succeeded(final UseCaseExecution execution) {
        return assemble(execution, AuditResult.SUCCESS, null);
    }

    AuditEvent failed(final UseCaseExecution execution, final Throwable failure) {
        final var error = AuditError.of(failure);
        return assemble(execution, error.category().result(), error);
    }

    private AuditEvent assemble(final UseCaseExecution execution, final AuditResult result, final AuditError error) {
        final var actor = execution.actor();
        final var span = execution.span();
        return new AuditEvent(eventIds.next(), execution.startedAt(), execution.elapsedMillis(),
                tenants.current(), actor.tenant(), source.service(), source.version(),
                span.traceId(), span.spanId(), span.parentSpanId(),
                execution.useCase(), execution.useCaseVersion(),
                actor.subject(), actor.type(), actor.sid(), actor.jti(),
                result, error, execution.input());
    }
}
