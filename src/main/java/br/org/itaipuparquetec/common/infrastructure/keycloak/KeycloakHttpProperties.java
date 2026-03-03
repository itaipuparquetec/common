package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.http")
public record KeycloakHttpProperties(
        int connectTimeoutMs,
        int readTimeoutMs
) {}
