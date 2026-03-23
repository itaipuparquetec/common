package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class KeycloakTokenProvider {

    private static final String TOKEN_ENDPOINT = "/realms/{realm}/protocol/openid-connect/token";
    private static final String GRANT_TYPE = "client_credentials";

    private final WebClient keycloakRawWebClient;
    private final KeycloakProperties props;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public KeycloakTokenProvider(@Qualifier("keycloakRawWebClient") WebClient keycloakRawWebClient,
                                 KeycloakProperties props) {
        this.keycloakRawWebClient = keycloakRawWebClient;
        this.props = props;
    }


    public Mono<String> getOrRefresh() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(props.tokenSafetyMarginSeconds()))) {
            return Mono.just(cachedToken);
        }
        return refresh();
    }

    private Mono<String> refresh() {
        return keycloakRawWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(TOKEN_ENDPOINT)
                        .build(props.realm()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", GRANT_TYPE)
                        .with("client_id", props.clientId())
                        .with("client_secret", props.clientSecret())
                )
                .retrieve()
                .bodyToMono(KeycloakTokenResponse.class)
                .handle((tokenResponse, sink) -> {
                    if (tokenResponse == null || tokenResponse.access_token() == null) {
                        sink.error(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Error getting token from keycloak"));
                        return;
                    }

                    this.cachedToken = tokenResponse.access_token();
                    this.expiresAt = Instant.now().plusSeconds(tokenResponse.expires_in());
                    sink.next(this.cachedToken);
                });
    }

    public void invalidate() {
        this.cachedToken = null;
        this.expiresAt = Instant.EPOCH;
    }
}
