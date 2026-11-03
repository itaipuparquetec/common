package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Builds the decoder of the tokens of one tenant, given its issuer. The default one fetches the public keys of the
 * issuer ({@link RemoteTenantJwtDecoderFactory}); the authorization server itself uses its own keys.
 */
@FunctionalInterface
public interface TenantJwtDecoderFactory {

    JwtDecoder decoderOf(String tenantName, String issuer);
}
