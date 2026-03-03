package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

class KeycloakHttpClientConfigTest {

    private KeycloakTokenProvider keycloakTokenProvider;
    private KeycloakHttpClientConfig keycloakHttpClientConfig;
    private ExchangeFunction exchangeFunction;
    private ArgumentCaptor<ClientRequest> clientRequestArgumentCaptor;
    private HttpServiceProxyFactory httpServiceProxyFactory;

    @BeforeEach
    void setup() {
        clientRequestArgumentCaptor = ArgumentCaptor.forClass(ClientRequest.class);
        keycloakTokenProvider = mock(KeycloakTokenProvider.class);
        keycloakHttpClientConfig = new KeycloakHttpClientConfig();
        exchangeFunction = mock(ExchangeFunction.class);
        final var webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        httpServiceProxyFactory = keycloakHttpClientConfig.keycloakHttpServiceProxyFactory(webClient, keycloakTokenProvider);
    }

    @Test
    void shouldRetryOnUnauthorized() {
        final var firstToken = UUID.randomUUID().toString();
        final var secondToken = UUID.randomUUID().toString();
        when(keycloakTokenProvider.getOrRefresh())
                .thenReturn(firstToken)
                .thenReturn(secondToken);
        when(exchangeFunction.exchange(any()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()));
        final var keycloakAdminApi = httpServiceProxyFactory.createClient(KeycloakAdminApi.class);

        final var ex = assertThrows(Exception.class, () ->
                keycloakAdminApi.listUsers("realm", true, "search", false, 0, 1));

        assertTrue(ex.getMessage().contains("401"));
        final var order = inOrder(keycloakTokenProvider);
        order.verify(keycloakTokenProvider).getOrRefresh();
        order.verify(keycloakTokenProvider).invalidate();
        order.verify(keycloakTokenProvider).getOrRefresh();
        order.verifyNoMoreInteractions();
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    void shouldRetryOnForbidden() {
        final var firstToken = UUID.randomUUID().toString();
        final var secondToken = UUID.randomUUID().toString();
        when(keycloakTokenProvider.getOrRefresh())
                .thenReturn(firstToken)
                .thenReturn(secondToken);
        when(exchangeFunction.exchange(any()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build()));
        final var keycloakAdminApi = httpServiceProxyFactory.createClient(KeycloakAdminApi.class);

        final var ex = assertThrows(Exception.class, () ->
                keycloakAdminApi.listUsers("realm", true, "search", false, 0, 1));

        assertTrue(ex.getMessage().contains("403"));
        final var order = inOrder(keycloakTokenProvider);
        order.verify(keycloakTokenProvider).getOrRefresh();
        order.verify(keycloakTokenProvider).invalidate();
        order.verify(keycloakTokenProvider).getOrRefresh();
        order.verifyNoMoreInteractions();
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    void shouldNotRetryOnBadRequest() {
        final var firstToken = UUID.randomUUID().toString();
        when(keycloakTokenProvider.getOrRefresh())
                .thenReturn(firstToken);
        when(exchangeFunction.exchange(any()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build()));
        final var keycloakAdminApi = httpServiceProxyFactory.createClient(KeycloakAdminApi.class);

        final var exception = assertThrows(Exception.class, () ->
                keycloakAdminApi.listUsers(
                        "realm",
                        true,
                        "search",
                        false,
                        0,
                        1
                ));

        assertTrue(exception.getMessage().contains("400"));
        final var order = inOrder(keycloakTokenProvider);
        order.verify(keycloakTokenProvider).getOrRefresh();
        order.verifyNoMoreInteractions();
        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void shouldReturnEmptyList() {
        final String baseUrl = "https://keycloak.local";
        final String searchUrl = "/admin/realms/realm/users";
        final KeycloakProperties keycloakProperties = mock(KeycloakProperties.class);
        final KeycloakHttpProperties keycloakHttpProperties = mock(KeycloakHttpProperties.class);
        when(keycloakProperties.authServerUrl()).thenReturn(baseUrl);
        when(keycloakHttpProperties.readTimeoutMs()).thenReturn(2000);
        when(keycloakHttpProperties.connectTimeoutMs()).thenReturn(1500);
        final var webClientRaw = keycloakHttpClientConfig
                .keycloakRawWebClient(keycloakProperties, keycloakHttpProperties);
        final var clientResponseMock = mock(ClientResponse.class);
        when(clientResponseMock.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponseMock.bodyToMono(String.class)).thenReturn(Mono.just("[]"));
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponseMock));
        final var webClientMocked = webClientRaw.mutate().exchangeFunction(exchangeFunction).build();

        var response = webClientMocked.get()
                .uri(searchUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        assertEquals("[]", response);
        verify(exchangeFunction, times(1)).exchange(clientRequestArgumentCaptor.capture());
        final var clientRequest = clientRequestArgumentCaptor.getValue();
        assertEquals(HttpMethod.GET, clientRequest.method());
    }
}
