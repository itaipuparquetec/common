package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

/**
 * One issuer per tenant (ADR-035), enabled by {@code hubti.auth.issuer-base-uri}: the resource server resolver that
 * only accepts tokens of active tenants and, with {@code hubti.auth.service-client-id}, the per-tenant token of the
 * microservice.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "hubti.auth", name = "issuer-base-uri")
@EnableConfigurationProperties(TenantIssuerProperties.class)
public class TenantIssuerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantIssuers tenantIssuers(final TenantIssuerProperties properties) {
        return new TenantIssuers(properties.issuerBaseUri());
    }

    @Bean
    @ConditionalOnMissingBean
    public ActiveTenantDirectory activeTenantDirectory(final TenantIssuerProperties properties) {
        return new TenantServiceActiveTenantDirectory(RestClient.builder().baseUrl(properties.tenantsUrl()).build());
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantJwtDecoderFactory tenantJwtDecoderFactory() {
        return new RemoteTenantJwtDecoderFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantIssuerAuthenticationManagerResolver tenantIssuerAuthenticationManagerResolver(
            final TenantIssuers tenantIssuers, final ActiveTenantDirectory activeTenantDirectory,
            final TenantJwtDecoderFactory tenantJwtDecoderFactory) {
        return new TenantIssuerAuthenticationManagerResolver(tenantIssuers, activeTenantDirectory,
                tenantJwtDecoderFactory, ScopeAuthenticationConverter.ofScopes());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hubti.auth", name = "service-client-id")
    public TenantServiceTokenProvider tenantServiceTokenProvider(final TenantIssuers tenantIssuers,
                                                                 final TenantIssuerProperties properties) {
        return new TenantServiceTokenProvider(tenantIssuers, RestClient.create(),
                properties.serviceClientId(), properties.serviceClientSecret(),
                Duration.ofSeconds(properties.tokenSafetyMarginSeconds()), Clock.systemUTC());
    }
}
