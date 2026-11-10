package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TenantServiceActiveTenantsSourceTest {

    private static final String PAGE = "http://gateway/api/tenants/v1/active-tenants?page=%d&size=100";

    private final TenantServiceTokenProvider tokenProvider = mock(TenantServiceTokenProvider.class);
    private MockRestServiceServer tenantService;
    private TenantServiceActiveTenantsSource source;

    @BeforeEach
    void setUp() {
        final var builder = RestClient.builder().baseUrl("http://gateway/api/tenants");
        tenantService = MockRestServiceServer.bindTo(builder).build();
        source = new TenantServiceActiveTenantsSource(builder.build(), tokenProvider);
        when(tokenProvider.tokenOf("hubti")).thenReturn("central-token");
    }

    @Test
    void readsEveryPageOfTheActiveTenantsWithTheTokenOfTheCentralTenant() {
        expectPage(0, "{\"content\":[{\"name\":\"acme_tenant\"},{\"name\":\"beta_tenant\"}],\"totalPages\":2}");
        expectPage(1, "{\"content\":[{\"name\":\"gamma_tenant\",\"label\":\"Gamma\"}],\"totalPages\":2}");
        final var names = new ArrayList<String>();

        source.forEachActiveTenant(names::add);

        assertThat(names).containsExactly("acme_tenant", "beta_tenant", "gamma_tenant");
        tenantService.verify();
    }

    @Test
    void refusesAnAnswerWithoutAPage() {
        expectPage(0, "{}");

        assertThatThrownBy(() -> source.forEachActiveTenant(name -> { }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("page 0");
    }

    private void expectPage(final int page, final String body) {
        tenantService.expect(requestTo(String.format(PAGE, page)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer central-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
