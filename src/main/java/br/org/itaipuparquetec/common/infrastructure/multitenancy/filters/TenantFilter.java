package br.org.itaipuparquetec.common.infrastructure.multitenancy.filters;

import br.org.itaipuparquetec.common.application.services.AuthenticationService;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.TenantIdentifierServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final AuthenticationService authenticationService;
    private final TenantIdentifierServiceImpl tenantIdentifierService;
    private final List<String> ignoredPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(@NonNull final HttpServletRequest request) {
        final var path = pathWithinApplicationOf(request);
        return ignoredPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest request,
                                    @NonNull final HttpServletResponse response,
                                    @NonNull final FilterChain filterChain) throws ServletException, IOException {
        final var tenantName = authenticationService.getTenantName();
        if (tenantName.equals("hubti") && request.getHeader("tenant_name") != null) {
            tenantIdentifierService.setTenantId(request.getHeader("tenant_name"));
        } else {
            tenantIdentifierService.setTenantId(tenantName);
        }
        filterChain.doFilter(request, response);
    }

    private String pathWithinApplicationOf(final HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}