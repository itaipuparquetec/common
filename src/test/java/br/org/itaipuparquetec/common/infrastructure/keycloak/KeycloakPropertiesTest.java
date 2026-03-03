package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakPropertiesTest {

    @Test
    void shouldBindPropertiesSuccessfully() {
        final var authServerUrl = "https://localhost";
        final var realm = "realm";
        final var clientId = "clientId";
        final var clientSecret = "clientSecret";
        final var tokenSafetyMarginSeconds = 30;
        final var propsMap = Map.of(
                "keycloak.auth-server-url", authServerUrl,
                "keycloak.realm", realm,
                "keycloak.client-id", clientId,
                "keycloak.client-secret", clientSecret,
                "keycloak.token-safety-margin-seconds", tokenSafetyMarginSeconds
        );
        final var binder = new Binder(new MapConfigurationPropertySource(propsMap));

        final KeycloakProperties keycloakProperties = binder.bind("keycloak", KeycloakProperties.class).get();

        assertEquals(authServerUrl, keycloakProperties.authServerUrl());
        assertEquals(realm, keycloakProperties.realm());
        assertEquals(clientId, keycloakProperties.clientId());
        assertEquals(clientSecret, keycloakProperties.clientSecret());
        assertEquals(tokenSafetyMarginSeconds, keycloakProperties.tokenSafetyMarginSeconds());
    }
}
