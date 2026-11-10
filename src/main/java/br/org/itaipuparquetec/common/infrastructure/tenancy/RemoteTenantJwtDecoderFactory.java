package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decodes the tokens of a tenant with the public keys its issuer publishes ({@code <issuer>/oauth2/jwks}).
 */
public class RemoteTenantJwtDecoderFactory implements TenantJwtDecoderFactory {

    static final String TENANT_NAME_CLAIM = "tenant_name";

    @Override
    public JwtDecoder decoderOf(final String tenantName, final String issuer) {
        final var decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/oauth2/jwks").build();
        decoder.setJwtValidator(validatorOf(tenantName, issuer));
        return decoder;
    }

    /**
     * Besides the default checks and the issuer, the tenant claim must be the tenant of the issuer.
     */
    public static OAuth2TokenValidator<Jwt> validatorOf(final String tenantName, final String issuer) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<String>(TENANT_NAME_CLAIM, tenantName::equals));
    }
}
