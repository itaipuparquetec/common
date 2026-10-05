package br.org.itaipuparquetec.common.infrastructure.audit.trail.context;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Reads identity claims from the current JWT: {@code sub} (immutable actor identifier), {@code sid} (session)
 * and {@code jti} (unique token identifier). Returns empty when there is no authenticated JWT (e.g. system
 * flows), so the audit trail degrades gracefully instead of failing the use case.
 */
public class TokenClaimsReader {

    public Optional<String> claim(final String name) {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return Optional.ofNullable(jwt.getToken().getClaimAsString(name));
        }
        return Optional.empty();
    }
}
