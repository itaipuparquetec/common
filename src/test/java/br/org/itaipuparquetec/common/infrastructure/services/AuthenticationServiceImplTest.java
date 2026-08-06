package br.org.itaipuparquetec.common.infrastructure.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;

    @Mock
    private Jwt jwt;

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        lenient().when(jwtAuthenticationToken.getToken()).thenReturn(jwt);

        authenticationService = new AuthenticationServiceImpl();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnSub() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("sub", "user-123"));

        assertThat(authenticationService.getSub()).isEqualTo("user-123");
    }

    @Test
    void shouldReturnName() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("name", "Emanuel Victor"));

        assertThat(authenticationService.getName()).isEqualTo("Emanuel Victor");
    }

    @Test
    void shouldReturnEmail() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("email", "emanuel@example.com"));

        assertThat(authenticationService.getEmail()).isEqualTo("emanuel@example.com");
    }

    @Test
    void shouldReturnGivenName() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("given_name", "Emanuel"));

        assertThat(authenticationService.getGivenName()).isEqualTo("Emanuel");
    }

    @Test
    void shouldReturnFamilyName() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("family_name", "Fonseca"));

        assertThat(authenticationService.getFamilyName()).isEqualTo("Fonseca");
    }

    @Test
    void shouldReturnPreferredUsername() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("preferred_username", "emanuelf"));

        assertThat(authenticationService.getPreferredUsername()).isEqualTo("emanuelf");
    }

    @Test
    void shouldReturnUsernameAsPreferredUsername() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("preferred_username", "emanuelf"));

        assertThat(authenticationService.getUsername()).isEqualTo("emanuelf");
    }

    @Test
    void shouldReturnScopeAsStream() {
        lenient().when(jwt.getClaimAsString("scope")).thenReturn("read write admin");

        final var scopes = authenticationService.getScope().toList();

        assertThat(scopes).containsExactly("read", "write", "admin");
    }

    @Test
    void shouldReturnSingleScope() {
        lenient().when(jwt.getClaimAsString("scope")).thenReturn("read");

        final var scopes = authenticationService.getScope().toList();

        assertThat(scopes).containsExactly("read");
    }

    @Test
    void shouldReturnTenantName() {
        lenient().when(jwt.getClaims()).thenReturn(Map.of("tenant_name", "tenant-123"));

        assertThat(authenticationService.getTenantName()).isEqualTo("tenant-123");
    }
}