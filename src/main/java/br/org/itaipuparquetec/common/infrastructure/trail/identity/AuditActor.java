package br.org.itaipuparquetec.common.infrastructure.trail.identity;

/**
 * Who triggered a use case execution, as read from the current JWT.
 *
 * @param subject the {@code sub} claim, or {@code null} without JWT
 * @param sid     the {@code sid} claim (session), or {@code null}
 * @param jti     the {@code jti} claim (token identifier), or {@code null}
 * @param tenant  the {@code tenant_name} claim, or {@code null}
 * @param type    nature of the actor
 */
public record AuditActor(String subject, String sid, String jti, String tenant, AuditActorType type) {
}
