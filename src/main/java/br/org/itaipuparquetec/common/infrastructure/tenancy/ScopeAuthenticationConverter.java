package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * The permissions of a Hubti token are its {@code scope} claim, without prefix.
 */
public final class ScopeAuthenticationConverter {

    private ScopeAuthenticationConverter() {
    }

    public static JwtAuthenticationConverter ofScopes() {
        final var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("scope");
        authoritiesConverter.setAuthorityPrefix("");
        final var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
