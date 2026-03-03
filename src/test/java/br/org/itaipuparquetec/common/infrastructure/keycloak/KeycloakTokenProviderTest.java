package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.*;

import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakTokenProviderTest {

    private static final String REALM = "realm";
    private static final String KEYCLOAK_TOKEN_ERROR =
            HttpStatus.BAD_REQUEST.value() + " Error getting token from keycloak";

    private ExchangeFunction exchangeFunction;
    private KeycloakTokenProvider keycloakTokenProvider;
    private KeycloakProperties keycloakProperties;

    @BeforeEach
    void setup() {
        exchangeFunction = mock(ExchangeFunction.class);
        keycloakProperties = mock(KeycloakProperties.class);
        final WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        keycloakTokenProvider = new KeycloakTokenProvider(webClient, keycloakProperties);

        when(keycloakProperties.realm()).thenReturn(REALM);
        when(keycloakProperties.clientId()).thenReturn("client-id");
        when(keycloakProperties.clientSecret()).thenReturn("client-secret");
    }

    @Test
    void shouldGetFirstTokenAndDontUpdateWhenOfSafetyMargin() {
        final String token = UUID.randomUUID().toString();
        final ClientResponse clientResponseMock = mock(ClientResponse.class);
        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponseMock.bodyToMono(KeycloakTokenResponse.class))
                .thenReturn(Mono.just(new KeycloakTokenResponse(token, 3600)));
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponseMock));


        final String firstToken = keycloakTokenProvider.getOrRefresh();
        final String secondToken = keycloakTokenProvider.getOrRefresh();

        assertEquals(token, firstToken);
        assertEquals(token, secondToken);
        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void shouldGetFirstTokenAndUpdateWhenOnSafetyMargin() {
        final String firstToken = UUID.randomUUID().toString();
        final String secondToken = UUID.randomUUID().toString();
        final ClientResponse clientResponseMock = mock(ClientResponse.class);
        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponseMock.bodyToMono(KeycloakTokenResponse.class))
                .thenReturn(Mono.just(new KeycloakTokenResponse(firstToken, 10)))
                .thenReturn(Mono.just(new KeycloakTokenResponse(secondToken, 10)));
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponseMock));
        when(keycloakProperties.tokenSafetyMarginSeconds()).thenReturn(30);

        final String firstTokenGet = keycloakTokenProvider.getOrRefresh();
        final String secondTokenGet = keycloakTokenProvider.getOrRefresh();

        assertEquals(firstTokenGet, firstToken);
        assertEquals(secondTokenGet, secondToken);
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    void shouldGetFirstTokenAndUpdateWhenInvalidateToken() {
        final String firstToken = UUID.randomUUID().toString();
        final String secondToken = UUID.randomUUID().toString();
        final ClientResponse clientResponseMock = mock(ClientResponse.class);
        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponseMock.bodyToMono(KeycloakTokenResponse.class))
                .thenReturn(Mono.just(new KeycloakTokenResponse(firstToken, 3600)))
                .thenReturn(Mono.just(new KeycloakTokenResponse(secondToken, 3600)));
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponseMock));

        final String firstTokenGet = keycloakTokenProvider.getOrRefresh();
        keycloakTokenProvider.invalidate();
        final String secondTokenGet = keycloakTokenProvider.getOrRefresh();

        assertEquals(firstTokenGet, firstToken);
        assertEquals(secondTokenGet, secondToken);
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    void shouldReturnEmptyToken() {
        final ClientResponse clientResponseMock = mock(ClientResponse.class);
        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponseMock.bodyToMono(KeycloakTokenResponse.class))
                .thenReturn(Mono.just(new KeycloakTokenResponse(null, 3600)));
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponseMock));

        final HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, keycloakTokenProvider::getOrRefresh);

        assertEquals(KEYCLOAK_TOKEN_ERROR, ex.getMessage());
        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void shouldReturnEmptyResponse() {
        final ClientResponse clientResponseMock = mock(ClientResponse.class);
        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponseMock.bodyToMono(KeycloakTokenResponse.class))
                .thenReturn(Mono.empty());
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponseMock));

        final HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, keycloakTokenProvider::getOrRefresh);

        assertEquals(KEYCLOAK_TOKEN_ERROR, ex.getMessage());
        verify(exchangeFunction, times(1)).exchange(any());
    }
}
