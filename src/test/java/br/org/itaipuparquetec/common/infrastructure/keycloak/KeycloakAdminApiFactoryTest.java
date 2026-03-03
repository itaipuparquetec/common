package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.junit.jupiter.api.Test;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeycloakAdminApiFactoryTest {

    @Test
    void shouldCreateFactorySuccessfully() {
        final var httpServiceProxyFactory = mock(HttpServiceProxyFactory.class);
        final var keycloakAdminApi = mock(KeycloakAdminApi.class);
        when(httpServiceProxyFactory.createClient(KeycloakAdminApi.class)).thenReturn(keycloakAdminApi);
        final var keycloakAdminApiFactory = new KeycloakAdminApiFactory();

        final var client = keycloakAdminApiFactory.keycloakAdminApi(httpServiceProxyFactory);

        assertSame(keycloakAdminApi, client);
        verify(httpServiceProxyFactory, times(1)).createClient(KeycloakAdminApi.class);
        verifyNoMoreInteractions(httpServiceProxyFactory);
    }

    @Test
    void shouldThrowExceptionOnFactoryFail() {
        final var httpServiceProxyFactory = mock(HttpServiceProxyFactory.class);
        when(httpServiceProxyFactory.createClient(KeycloakAdminApi.class))
                .thenThrow(new IllegalStateException("Fail"));
        final var keycloakAdminApiFactory = new KeycloakAdminApiFactory();

        final var illegalStateException = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> keycloakAdminApiFactory.keycloakAdminApi(httpServiceProxyFactory)
        );

        assertEquals("Fail", illegalStateException.getMessage());
        verify(httpServiceProxyFactory, times(1)).createClient(KeycloakAdminApi.class);
        verifyNoMoreInteractions(httpServiceProxyFactory);
    }
}
