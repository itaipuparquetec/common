package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TenantServiceActiveTenantDirectoryTest {

    private MockRestServiceServer tenantService;
    private TenantServiceActiveTenantDirectory directory;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder().baseUrl("http://tenants");
        tenantService = MockRestServiceServer.bindTo(builder).build();
        directory = new TenantServiceActiveTenantDirectory(builder.build());
    }

    @Test
    void anActiveTenantIsAskedOnceAndRemembered() {
        tenantService.expect(once(), requestTo("http://tenants/v1/tenants/active/acme_tenant")).andRespond(withSuccess());

        final var first = directory.isActive("acme_tenant");
        final var second = directory.isActive("acme_tenant");

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        tenantService.verify();
    }

    @Test
    void aTenantTheTenantServiceDoesNotFindIsNotActive() {
        tenantService.expect(requestTo("http://tenants/v1/tenants/active/gone_tenant")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(directory.isActive("gone_tenant")).isFalse();
    }

    @Test
    void anUnreachableTenantServiceRefusesATenantItNeverAnswered() {
        tenantService.expect(requestTo("http://tenants/v1/tenants/active/acme_tenant")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(directory.isActive("acme_tenant")).isFalse();
    }

    @Test
    void aClientErrorOtherThanNotFoundRefusesTheTenant() {
        tenantService.expect(requestTo("http://tenants/v1/tenants/active/acme_tenant")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(directory.isActive("acme_tenant")).isFalse();
    }
}
