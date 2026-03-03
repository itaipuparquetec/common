package br.org.itaipuparquetec.common.infrastructure.keycloak;

public record KeycloakTokenResponse(
    String access_token,
    Integer expires_in
) {
    public KeycloakTokenResponse {
        if (expires_in == null) {
            expires_in = 60;
        }
    }
}
