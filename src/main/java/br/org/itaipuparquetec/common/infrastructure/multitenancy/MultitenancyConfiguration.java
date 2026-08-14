package br.org.itaipuparquetec.common.infrastructure.multitenancy;

import br.org.itaipuparquetec.common.application.services.AuthenticationService;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantConnectionInfoProvider;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantPoolFactory;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.events.ReceiveNewTenantCreatedEventImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.filters.TenantFilter;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.ConnectionProvider;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.PostgreSQLMigrationServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Registers the standalone multitenancy instrumentation.
 * It is only enabled when {@code hubti.multitenancy.enabled=true}, allowing microservices
 * with their own instrumentation (e.g. hubti-groups-api) to not inherit these beans.
 */
@Configuration
@ConditionalOnProperty(prefix = "hubti.multitenancy", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MultitenancyProperties.class)
public class MultitenancyConfiguration {

    @Bean
    public TenantConnectionInfoProvider tenantConnectionInfoProvider(
            @Value("${spring.datasource.url}") final String centralUrl,
            @Value("${spring.datasource.username}") final String username,
            @Value("${spring.datasource.password}") final String password) {
        return new TenantConnectionInfoProvider(centralUrl, username, password);
    }

    @Bean
    public TenantPoolFactory tenantPoolFactory(
            @Value("${spring.datasource.driver-class-name}") final String driverClassName,
            @Value("${spring.datasource.hikari.schema}") final String schema,
            @Value("${spring.datasource.hikari.connection-init-sql}") final String connectionInitSql,
            @Value("${spring.datasource.hikari.maximum-pool-size}") final int maximumPoolSize,
            @Value("${spring.datasource.hikari.minimum-idle}") final int minimumIdle,
            @Value("${spring.datasource.hikari.idle-timeout}") final long idleTimeout,
            final TenantConnectionInfoProvider tenantConnectionInfoProvider) {
        return new TenantPoolFactory(driverClassName, schema, connectionInitSql,
                maximumPoolSize, minimumIdle, idleTimeout, tenantConnectionInfoProvider);
    }

    @Bean
    public TenantDataSourceRegistryImpl tenantDataSourceRegistryImpl(final TenantPoolFactory tenantPoolFactory) {
        return new TenantDataSourceRegistryImpl(tenantPoolFactory);
    }

    @Bean
    @ConditionalOnMissingBean(PostgreSQLMigrationServiceImpl.class)
    public PostgreSQLMigrationServiceImpl postgreSQLMigrationServiceImpl(
            final TenantDataSourceRegistryImpl tenantDataSourceRegistryImpl,
            final TenantPoolFactory tenantPoolFactory) {
        return new PostgreSQLMigrationServiceImpl(tenantDataSourceRegistryImpl, tenantPoolFactory);
    }

    @Bean
    @ConditionalOnMissingBean(TenantIdentifierServiceImpl.class)
    public TenantIdentifierServiceImpl tenantIdentifierServiceImpl() {
        return new TenantIdentifierServiceImpl();
    }

    @Bean
    public ConnectionProvider connectionProvider(final TenantDataSourceRegistryImpl tenantDataSourceRegistryImpl) {
        return new ConnectionProvider(tenantDataSourceRegistryImpl);
    }

    @Bean
    @DependsOn("postgreSQLMigrationServiceImpl")
    public RoutingDataSourceService routingDataSourceService(
            final TenantIdentifierServiceImpl tenantIdentifierServiceImpl,
            final TenantDataSourceRegistryImpl tenantDataSourceRegistryImpl) {
        return new RoutingDataSourceService(tenantIdentifierServiceImpl, tenantDataSourceRegistryImpl);
    }

    @Bean
    public ReceiveNewTenantCreatedEventImpl receiveNewTenantCreatedEventImpl(
            final KafkaTemplate<String, String> kafkaTemplate,
            final PostgreSQLMigrationServiceImpl postgreSQLMigrationServiceImpl) {
        return new ReceiveNewTenantCreatedEventImpl(kafkaTemplate, postgreSQLMigrationServiceImpl);
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter(
            final AuthenticationService authenticationService,
            final TenantIdentifierServiceImpl tenantIdentifierServiceImpl,
            final MultitenancyProperties multitenancyProperties) {
        final FilterRegistrationBean<TenantFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantFilter(
                authenticationService, tenantIdentifierServiceImpl, multitenancyProperties.ignoredPaths()));
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
