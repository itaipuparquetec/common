package br.org.itaipuparquetec.common.infrastructure.services;

import br.org.itaipuparquetec.common.application.services.AuthenticationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.stream.Stream;

public class AuthenticationServiceImpl implements AuthenticationService {

    @Override
    public String getSub() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((JwtAuthenticationToken) auth).getToken().getClaims().get("sub").toString();
    }

    @Override
    public String getName() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((JwtAuthenticationToken) auth).getToken().getClaims().get("name").toString();
    }

    @Override
    public String getEmail() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((JwtAuthenticationToken) auth).getToken().getClaims().get("email").toString();
    }

    @Override
    public String getUsername() {
        return getPreferredUsername();
    }

    @Override
    public String getGivenName() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((JwtAuthenticationToken) auth).getToken().getClaims().get("given_name").toString();
    }

    @Override
    public String getFamilyName() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((JwtAuthenticationToken) auth).getToken().getClaims().get("family_name").toString();
    }

    @Override
    public String getPreferredUsername() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((JwtAuthenticationToken) auth).getToken().getClaims().get("preferred_username").toString();
    }

    @Override
    public Stream<String> getScope() {
        final var auth =
                (JwtAuthenticationToken) SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        final var scope = auth.getToken().getClaimAsString("scope");

        return Arrays.stream(scope.split(" "));
    }

    @Override
    public String getTenantName() {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((JwtAuthenticationToken) auth).getToken().getClaims().get("tenant_name").toString();
    }
}
