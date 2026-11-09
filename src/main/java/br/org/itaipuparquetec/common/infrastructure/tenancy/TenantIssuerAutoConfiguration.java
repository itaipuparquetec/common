package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * One issuer per tenant (ADR-035), enabled by {@code hubti.auth.issuer-base-uri}: the resource server resolver that
 * only accepts tokens of active tenants, kept in memory (loaded at startup, updated by events, never asked per
 * request) and, with {@code hubti.auth.service-client-id}, the per-tenant token of the microservice.
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
    public InMemoryActiveTenantDirectory activeTenantDirectory() {
        return new InMemoryActiveTenantDirectory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ActiveTenantsListener activeTenantsListener(final InMemoryActiveTenantDirectory activeTenantDirectory) {
        return new ActiveTenantsListener(activeTenantDirectory);
    }

    /**
     * Without the credential of the microservice the list of active tenants cannot be read: then only the central
     * tenant and the tenants of the events are accepted, and the service must provide its own {@link ActiveTenantsSource}.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "hubti.auth", name = {"service-client-id", "tenants-url"})
    public ActiveTenantsSource activeTenantsSource(final TenantIssuerProperties properties,
                                                   final TenantServiceTokenProvider tenantServiceTokenProvider) {
        return new TenantServiceActiveTenantsSource(RestClient.builder().baseUrl(properties.tenantsUrl()).build(),
                tenantServiceTokenProvider);
    }

    @Bean
    public ActiveTenantsLoader activeTenantsLoader(final ObjectProvider<ActiveTenantsSource> activeTenantsSource,
                                                   final InMemoryActiveTenantDirectory activeTenantDirectory) {
        return new ActiveTenantsLoader(activeTenantsSource.getIfAvailable(() -> consumer -> { }), activeTenantDirectory,
                Executors.newSingleThreadScheduledExecutor(TenantIssuerAutoConfiguration::daemonThreadOf));
    }

    static Thread daemonThreadOf(final Runnable loading) {
        final var thread = new Thread(loading, "active-tenants-loader");
        thread.setDaemon(true);
        return thread;
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
