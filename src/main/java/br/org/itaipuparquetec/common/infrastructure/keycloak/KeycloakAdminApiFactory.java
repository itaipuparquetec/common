package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class KeycloakAdminApiFactory {

    @Bean
    KeycloakAdminApi keycloakAdminApi(final HttpServiceProxyFactory factory) {
        return factory.createClient(KeycloakAdminApi.class);
    }
}
