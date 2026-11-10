package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantIssuerAuthenticationManagerResolverTest {

    private static final String BASE = "https://hubti.example.org";
    private static final String ACME_ISSUER = BASE + "/acme_tenant";

    private final List<String> decodersBuilt = new ArrayList<>();
    private TenantIssuerAuthenticationManagerResolver resolver;

    @BeforeEach
    void setUp() {
        final ActiveTenantDirectory directory = Set.of("acme_tenant")::contains;
        resolver = new TenantIssuerAuthenticationManagerResolver(new TenantIssuers(BASE), directory,
                this::fakeDecoderOf, ScopeAuthenticationConverter.ofScopes());
    }

    @Test
    void authenticatesATokenOfAnActiveTenantWithTheScopesAsPermissions() {
        final var request = requestWithTokenOf(ACME_ISSUER);

        final var authentication = resolver.resolve(request).authenticate(new BearerTokenAuthenticationToken(tokenValueOf(ACME_ISSUER)));

        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("root.flows");
    }

    @Test
    void buildsTheDecoderOfAnIssuerOnlyOnce() {
        resolver.managerOf(ACME_ISSUER);
        resolver.managerOf(ACME_ISSUER);

        assertThat(decodersBuilt).containsExactly("acme_tenant");
    }

    @Test
    void refusesTheIssuerOfAnInactiveTenant() {
        assertThat(resolver.managerOf(BASE + "/gone_tenant")).isNull();
    }

    @Test
    void refusesAnIssuerThatIsNotOfATenant() {
        assertThat(resolver.managerOf("https://evil.example.org/acme_tenant")).isNull();
    }

    @Test
    void refusesATokenOfAnUnknownIssuerAsInvalid() {
        final var request = requestWithTokenOf("https://evil.example.org/acme_tenant");
        final var token = new BearerTokenAuthenticationToken(tokenValueOf("https://evil.example.org/acme_tenant"));
        final var authenticationManager = resolver.resolve(request);

        assertThatThrownBy(() -> authenticationManager.authenticate(token)).isInstanceOf(InvalidBearerTokenException.class);
    }

    private JwtDecoder fakeDecoderOf(final String tenantName, final String issuer) {
        decodersBuilt.add(tenantName);
        return tokenValue -> Jwt.withTokenValue(tokenValue).header("alg", "none")
                .issuer(issuer).claim("tenant_name", tenantName).claim("scope", "root.flows")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .build();
    }

    private static MockHttpServletRequest requestWithTokenOf(final String issuer) {
        final var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenValueOf(issuer));
        return request;
    }

    /**
     * An unsigned JWT: the resolver only reads its issuer, the fake decoder never checks a signature.
     */
    private static String tokenValueOf(final String issuer) {
        final var encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        final var header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes());
        final var payload = encoder.encodeToString(("{\"iss\":\"" + issuer + "\"}").getBytes());
        return header + "." + payload + ".";
    }
}
