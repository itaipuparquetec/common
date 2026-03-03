package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakTokenResponseTest {

    @Test
    void shouldCreateSuccessfully() {
        final String token = "token";
        final int expires_in = 120;

        final KeycloakTokenResponse response = new KeycloakTokenResponse(token, expires_in);

        assertThat(response.access_token()).isEqualTo(token);
        assertThat(response.expires_in()).isEqualTo(expires_in);
    }

    @Test
    void shouldCreateWithDefaultExpiresInSuccessfully() {
        final String token = "token";
        final int expires_in = 60;

        final KeycloakTokenResponse response = new KeycloakTokenResponse(token, null);

        assertThat(response.access_token()).isEqualTo(token);
        assertThat(response.expires_in()).isEqualTo(expires_in);
    }

    @Test
    void shouldCreateWithEmptyTokenSuccessfully() {
        final KeycloakTokenResponse response = new KeycloakTokenResponse(null, null);

        assertThat(response.access_token()).isNull();
        assertThat(response.expires_in()).isEqualTo(60);
    }
}
