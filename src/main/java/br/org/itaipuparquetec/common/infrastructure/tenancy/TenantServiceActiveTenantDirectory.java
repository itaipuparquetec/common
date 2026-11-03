package br.org.itaipuparquetec.common.infrastructure.tenancy;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Asks the TenantService whether a tenant is active ({@code GET /v1/tenants/active/{name}}). The answer is kept for a
 * minute and refreshed in the background; while the TenantService is unreachable, the last answer is kept, so its
 * failure does not refuse the tokens of the tenants already known.
 */
@Slf4j
public class TenantServiceActiveTenantDirectory implements ActiveTenantDirectory {

    private static final Duration REFRESH_AFTER = Duration.ofMinutes(1);
    private static final Duration EXPIRE_AFTER = Duration.ofHours(1);

    private final RestClient restClient;
    private final LoadingCache<String, Boolean> activeByTenant;

    public TenantServiceActiveTenantDirectory(final RestClient restClient) {
        this.restClient = restClient;
        this.activeByTenant = Caffeine.newBuilder()
                .refreshAfterWrite(REFRESH_AFTER)
                .expireAfterWrite(EXPIRE_AFTER)
                .build(this::askTheTenantService);
    }

    @Override
    public boolean isActive(final String tenantName) {
        try {
            return Boolean.TRUE.equals(activeByTenant.get(tenantName));
        } catch (final RuntimeException exception) {
            log.warn("Could not tell whether tenant '{}' is active; its tokens are refused", tenantName, exception);
            return false;
        }
    }

    private Boolean askTheTenantService(final String tenantName) {
        try {
            restClient.get().uri("/v1/tenants/active/{name}", tenantName).retrieve().toBodilessEntity();
            return true;
        } catch (final HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw exception;
        }
    }
}
