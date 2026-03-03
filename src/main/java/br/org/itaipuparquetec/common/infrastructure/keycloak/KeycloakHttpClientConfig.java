package br.org.itaipuparquetec.common.infrastructure.keycloak;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties({KeycloakProperties.class, KeycloakHttpProperties.class})
public class KeycloakHttpClientConfig {

    @Bean
    WebClient keycloakRawWebClient(
            final KeycloakProperties props,
            final KeycloakHttpProperties http
    ) {
        final HttpClient httpClient = HttpClient.create()
                .responseTimeout(java.time.Duration.ofMillis(http.readTimeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, http.connectTimeoutMs());

        return WebClient.builder()
                .baseUrl(props.authServerUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public HttpServiceProxyFactory keycloakHttpServiceProxyFactory(
            final WebClient keycloakWebClient,
            final KeycloakTokenProvider tokenProvider
    ) {
        final ExchangeFilterFunction authFilter = (request, next) -> {
            final ClientRequest authorized = createRequest(request, tokenProvider);
            return next.exchange(authorized).flatMap(response -> {
                if (response.statusCode().value() == 401 || response.statusCode().value() == 403) {
                    tokenProvider.invalidate();
                    final ClientRequest retried = createRequest(request, tokenProvider);
                    return next.exchange(retried);
                }
                return Mono.just(response);
            });
        };

        final WebClient authorizedForProxy = keycloakWebClient.mutate()
                .filters(fs -> fs.add(authFilter))
                .build();

        final WebClientAdapter adapter = WebClientAdapter.create(authorizedForProxy);
        return HttpServiceProxyFactory.builderFor(adapter).build();
    }

    private ClientRequest createRequest(final ClientRequest request, final KeycloakTokenProvider tokenProvider) {
        return ClientRequest.from(request)
                .headers(h -> h.setBearerAuth(tokenProvider.getOrRefresh()))
                .build();
    }
}
