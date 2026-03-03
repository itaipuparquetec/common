package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeycloakHttpPropertiesTest {

    @Test
    void shouldBindPropertiesSuccessfully() {
        final var connectTimeoutMs = 2000;
        final var readTimeoutMs = 3000;
        final var propsMap = Map.of(
                "keycloak.http.connect-timeout-ms", connectTimeoutMs,
                "keycloak.http.read-timeout-ms", readTimeoutMs
        );
        final var binder = new Binder(new MapConfigurationPropertySource(propsMap));

        final var keycloakHttpProperties = binder.bind("keycloak.http", KeycloakHttpProperties.class).get();

        assertEquals(connectTimeoutMs, keycloakHttpProperties.connectTimeoutMs());
        assertEquals(readTimeoutMs, keycloakHttpProperties.readTimeoutMs());
    }
}
