package br.org.itaipuparquetec.common.infrastructure.multitenancy.filters;

import br.org.itaipuparquetec.common.application.services.AuthenticationService;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.TenantIdentifierServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantFilterTest {

    private static final List<String> IGNORED_PATHS = List.of("/public/**", "/v1/accounts/authenticate");

    private AuthenticationService authenticationService;
    private TenantIdentifierServiceImpl tenantIdentifierService;
    private TenantFilter tenantFilter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        tenantIdentifierService = mock(TenantIdentifierServiceImpl.class);
        tenantFilter = new TenantFilter(authenticationService, tenantIdentifierService, IGNORED_PATHS);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void usesTheHeaderTenantWhenAuthenticatedAsHubtiAndHeaderIsPresent() throws Exception {
        when(authenticationService.getTenantName()).thenReturn("hubti");
        when(request.getHeader("tenant_name")).thenReturn("acme_tenant");

        tenantFilter.doFilterInternal(request, response, filterChain);

        verify(tenantIdentifierService).setTenantId("acme_tenant");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void fallsBackToTheAuthenticatedTenantWhenHubtiButHeaderIsAbsent() throws Exception {
        when(authenticationService.getTenantName()).thenReturn("hubti");
        when(request.getHeader("tenant_name")).thenReturn(null);

        tenantFilter.doFilterInternal(request, response, filterChain);

        verify(tenantIdentifierService).setTenantId("hubti");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usesTheAuthenticatedTenantWhenItIsNotHubti() throws Exception {
        when(authenticationService.getTenantName()).thenReturn("acme_tenant");

        tenantFilter.doFilterInternal(request, response, filterChain);

        verify(tenantIdentifierService).setTenantId("acme_tenant");
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest
    @CsvSource({
            "/public/signatures/abc, true",
            "/v1/accounts/authenticate, true",
            "/v1/users, false",
            "/, false"
    })
    void skipsFilteringOnlyForIgnoredPaths(final String uri, final boolean ignored) {
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");

        assertThat(tenantFilter.shouldNotFilter(request)).isEqualTo(ignored);
    }

    @Test
    void resolvesThePathRelativeToTheContextPath() {
        when(request.getRequestURI()).thenReturn("/api/public/signatures/abc");
        when(request.getContextPath()).thenReturn("/api");

        assertThat(tenantFilter.shouldNotFilter(request)).isTrue();
    }
}