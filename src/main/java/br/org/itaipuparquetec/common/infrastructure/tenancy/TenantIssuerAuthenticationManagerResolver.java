package br.org.itaipuparquetec.common.infrastructure.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource server with one issuer per tenant (ADR-035): a token is accepted only when its issuer is the issuer of an
 * active tenant, decoded with the keys of that tenant. Any other issuer is refused as an invalid token.
 */
public class TenantIssuerAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final TenantIssuers tenantIssuers;
    private final ActiveTenantDirectory activeTenantDirectory;
    private final TenantJwtDecoderFactory decoderFactory;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter;
    private final Map<String, AuthenticationManager> managersByIssuer = new ConcurrentHashMap<>();
    private final JwtIssuerAuthenticationManagerResolver delegate;

    public TenantIssuerAuthenticationManagerResolver(final TenantIssuers tenantIssuers,
                                                     final ActiveTenantDirectory activeTenantDirectory,
                                                     final TenantJwtDecoderFactory decoderFactory,
                                                     final Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter) {
        this.tenantIssuers = tenantIssuers;
        this.activeTenantDirectory = activeTenantDirectory;
        this.decoderFactory = decoderFactory;
        this.authenticationConverter = authenticationConverter;
        this.delegate = new JwtIssuerAuthenticationManagerResolver(this::managerOf);
    }

    @Override
    public AuthenticationManager resolve(final HttpServletRequest request) {
        return delegate.resolve(request);
    }

    AuthenticationManager managerOf(final String issuer) {
        return tenantIssuers.tenantOf(issuer)
                .filter(activeTenantDirectory::isActive)
                .map(tenantName -> managersByIssuer.computeIfAbsent(issuer, key -> newManagerOf(tenantName, key)))
                .orElse(null);
    }

    private AuthenticationManager newManagerOf(final String tenantName, final String issuer) {
        final var provider = new JwtAuthenticationProvider(decoderFactory.decoderOf(tenantName, issuer));
        provider.setJwtAuthenticationConverter(authenticationConverter);
        return provider::authenticate;
    }
}
