package br.org.itaipuparquetec.common.infrastructure.multitenancy.filters;

import br.org.itaipuparquetec.common.application.services.AuthenticationService;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.TenantIdentifierServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantFilterTest {

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
        tenantFilter = new TenantFilter(authenticationService, tenantIdentifierService);
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
}
