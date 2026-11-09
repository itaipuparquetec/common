package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIssuerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantIssuerAutoConfiguration.class));

    @Test
    void staysOffWithoutTheBaseOfTheIssuers() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(TenantIssuerAuthenticationManagerResolver.class));
    }

    @Test
    void registersTheResolverWithTheActiveTenantsInMemory() {
        contextRunner.withPropertyValues("hubti.auth.issuer-base-uri=https://groups.example.org", "hubti.auth.tenants-url=http://tenants")
                .run(context -> {
                    assertThat(context).hasSingleBean(TenantIssuerAuthenticationManagerResolver.class);
                    assertThat(context).hasSingleBean(InMemoryActiveTenantDirectory.class);
                    assertThat(context).hasSingleBean(ActiveTenantsListener.class);
                    assertThat(context).hasSingleBean(ActiveTenantsLoader.class);
                    assertThat(context).hasSingleBean(RemoteTenantJwtDecoderFactory.class);
                    assertThat(context).doesNotHaveBean(TenantServiceTokenProvider.class);
                    assertThat(context).doesNotHaveBean(ActiveTenantsSource.class);
                    assertThat(context.getBean(TenantIssuerProperties.class).tokenSafetyMarginSeconds()).isEqualTo(30);
                });
    }

    @Test
    void readsTheActiveTenantsFromTheTenantServiceWithTheServiceCredential() {
        contextRunner.withPropertyValues("hubti.auth.issuer-base-uri=https://groups.example.org", "hubti.auth.tenants-url=http://tenants",
                        "hubti.auth.service-client-id=hubti-service", "hubti.auth.service-client-secret=secret",
                        "hubti.auth.token-safety-margin-seconds=10")
                .run(context -> {
                    assertThat(context).hasSingleBean(TenantServiceTokenProvider.class);
                    assertThat(context).hasSingleBean(TenantServiceActiveTenantsSource.class);
                });
    }

    @Test
    void retriesTheLoadOnADaemonThreadThatNeverHoldsTheShutdown() {
        final var thread = TenantIssuerAutoConfiguration.daemonThreadOf(() -> { });

        assertThat(thread.isDaemon()).isTrue();
        assertThat(thread.getName()).isEqualTo("active-tenants-loader");
    }
}
