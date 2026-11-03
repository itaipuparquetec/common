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
    void registersTheResolverWithTheBaseOfTheIssuers() {
        contextRunner.withPropertyValues("hubti.auth.issuer-base-uri=https://sentry.example.org", "hubti.auth.tenants-url=http://tenants")
                .run(context -> {
                    assertThat(context).hasSingleBean(TenantIssuerAuthenticationManagerResolver.class);
                    assertThat(context).hasSingleBean(TenantServiceActiveTenantDirectory.class);
                    assertThat(context).hasSingleBean(RemoteTenantJwtDecoderFactory.class);
                    assertThat(context).doesNotHaveBean(TenantServiceTokenProvider.class);
                    assertThat(context.getBean(TenantIssuerProperties.class).tokenSafetyMarginSeconds()).isEqualTo(30);
                });
    }

    @Test
    void registersTheServiceTokenWithTheServiceCredential() {
        contextRunner.withPropertyValues("hubti.auth.issuer-base-uri=https://sentry.example.org", "hubti.auth.tenants-url=http://tenants",
                        "hubti.auth.service-client-id=hubti-service", "hubti.auth.service-client-secret=secret",
                        "hubti.auth.token-safety-margin-seconds=10")
                .run(context -> assertThat(context).hasSingleBean(TenantServiceTokenProvider.class));
    }
}
