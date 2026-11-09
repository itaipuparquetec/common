package br.org.itaipuparquetec.common.infrastructure.tenancy;

import br.org.itaipuparquetec.common.domain.pagination.PageResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.function.Consumer;

/**
 * Reads the active tenants from the TenantService ({@code GET /v1/active-tenants}, page by page), with the token of
 * the microservice in the central tenant, since that list is not open (ADR-035).
 */
public class TenantServiceActiveTenantsSource implements ActiveTenantsSource {

    static final int PAGE_SIZE = 100;
    private static final ParameterizedTypeReference<PageResponse<ActiveTenant>> PAGE_OF_TENANTS = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final TenantServiceTokenProvider tenantServiceTokenProvider;

    public TenantServiceActiveTenantsSource(final RestClient restClient, final TenantServiceTokenProvider tenantServiceTokenProvider) {
        this.restClient = restClient;
        this.tenantServiceTokenProvider = tenantServiceTokenProvider;
    }

    @Override
    public void forEachActiveTenant(final Consumer<String> tenantNameConsumer) {
        var page = 0;
        PageResponse<ActiveTenant> response;
        do {
            response = pageOf(page++);
            response.getContent().forEach(tenant -> tenantNameConsumer.accept(tenant.name()));
        } while (page < response.getTotalPages());
    }

    private PageResponse<ActiveTenant> pageOf(final int page) {
        final var response = restClient.get()
                .uri("/v1/active-tenants?page={page}&size={size}", page, PAGE_SIZE)
                .header("Authorization", "Bearer " + tenantServiceTokenProvider.tokenOf(InMemoryActiveTenantDirectory.CENTRAL_TENANT))
                .retrieve()
                .body(PAGE_OF_TENANTS);
        if (response == null || response.getContent() == null) {
            throw new IllegalStateException("Expected a page of active tenants from the TenantService, but got none (page " + page + ")");
        }
        return response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ActiveTenant(String name) {
    }
}
