package br.org.itaipuparquetec.common.infrastructure.blacklist;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JwtBlacklistFilterTest {

    private TokenBlacklist tokenBlacklist;
    private JwtBlacklistFilter jwtBlacklistFilter;

    @BeforeEach
    void setUp() {
        tokenBlacklist = mock(TokenBlacklist.class);
        jwtBlacklistFilter = new JwtBlacklistFilter(tokenBlacklist);
    }

    @Test
    void doFilterInternalWithoutAuthorizationHeader() throws ServletException, IOException {
        final var request = mock(HttpServletRequest.class);
        final var response = mock(HttpServletResponse.class);
        final var chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtBlacklistFilter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void doFilterInternalAndSendsUnauthorizedAndDoesNotCallChain() throws ServletException, IOException {
        final var request = mock(HttpServletRequest.class);
        final var response = mock(HttpServletResponse.class);
        final var chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.token.value");
        when(tokenBlacklist.isRevoked("abc.token.value")).thenReturn(true);

        jwtBlacklistFilter.doFilterInternal(request, response, chain);

        verify(response, times(1)).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token revogado"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternalNotRevoked() throws ServletException, IOException {
        final var request = mock(HttpServletRequest.class);
        final var response = mock(HttpServletResponse.class);
        final var chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer xyz");
        when(tokenBlacklist.isRevoked("xyz")).thenReturn(false);

        jwtBlacklistFilter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }
}


