package br.org.itaipuparquetec.common.infrastructure.trail.identity;

import br.org.itaipuparquetec.common.infrastructure.trail.context.AuditContext;
import br.org.itaipuparquetec.common.infrastructure.trail.context.TokenClaimsReader;

/**
 * Resolves the {@link AuditActor} of the current execution. A token with a session ({@code sid}) or a
 * {@code preferred_username} was issued to a user; a token with neither (client credentials) belongs to a
 * service; without token the actor is the system itself.
 */
public class AuditActorResolver {

    private static final String SUBJECT_CLAIM = "sub";
    private static final String TENANT_CLAIM = "tenant_name";
    private static final String USERNAME_CLAIM = "preferred_username";

    private final TokenClaimsReader claims;

    public AuditActorResolver(final TokenClaimsReader claims) {
        this.claims = claims;
    }

    public AuditActor current() {
        final var subject = claims.claim(SUBJECT_CLAIM).orElse(null);
        return new AuditActor(
                subject,
                claims.claim(AuditContext.SID).orElse(null),
                claims.claim(AuditContext.JTI).orElse(null),
                claims.claim(TENANT_CLAIM).orElse(null),
                typeOf(subject));
    }

    private AuditActorType typeOf(final String subject) {
        if (subject == null) {
            return AuditActorType.SYSTEM;
        }
        final var issuedToUser = claims.claim(AuditContext.SID).isPresent()
                || claims.claim(USERNAME_CLAIM).isPresent();
        return issuedToUser ? AuditActorType.USER : AuditActorType.SERVICE;
    }
}
