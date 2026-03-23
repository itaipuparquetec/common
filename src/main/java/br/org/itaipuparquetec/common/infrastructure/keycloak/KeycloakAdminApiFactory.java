package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class KeycloakAdminApiFactory {

    @Bean
    KeycloakAdminApi keycloakAdminApi(@Qualifier("keycloakHttpServiceProxyFactory")
                                      final HttpServiceProxyFactory keycloakHttpServiceProxyFactory) {
        return keycloakHttpServiceProxyFactory.createClient(KeycloakAdminApi.class);
    }
}
