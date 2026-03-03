package br.org.itaipuparquetec.common.infrastructure.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.stream.Stream;

import br.org.itaipuparquetec.common.infrastructure.keycloak.KeycloakAdminApi;
import br.org.itaipuparquetec.common.infrastructure.keycloak.KeycloakProperties;
import br.org.itaipuparquetec.common.application.services.ExternalUserService.ExternalUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.springframework.data.domain.*;

class ExternalUserServiceImplTest {

    private static final String REALM = "realm";

    private KeycloakAdminApi keycloakAdminApi;
    private ExternalUserServiceImpl service;

    @BeforeEach
    void setup() {
        final KeycloakProperties keycloakProperties = mock(KeycloakProperties.class);
        keycloakAdminApi = mock(KeycloakAdminApi.class);
        when(keycloakProperties.realm()).thenReturn(REALM);
        service = new ExternalUserServiceImpl(keycloakAdminApi, keycloakProperties);
    }

    @Test
    void shouldListUsersWithPaginationAndTotal() {
        final String search = "search";
        final Pageable pageable = PageRequest.of(0, 5);
        final long total = 1L;
        when(keycloakAdminApi.countUsers(REALM, search, false)).thenReturn(total);
        final String id = UUID.randomUUID().toString();
        final String username = "username";
        final String email = "email";
        final String firstName = "first name";
        final String lastName = "last name";
        final var mapExternalResponse = new HashMap<String, Object>();
        mapExternalResponse.put("username", username);
        mapExternalResponse.put("email", email);
        mapExternalResponse.put("firstName", firstName);
        mapExternalResponse.put("lastName", lastName);
        mapExternalResponse.put("id", id);
        final List<Map<String, Object>> rawUsers = List.of(mapExternalResponse);
        when(keycloakAdminApi.listUsers(REALM, true, search, false, 0, 5))
                .thenReturn(rawUsers);

        final Page<ExternalUserResponse> page = service.listExternalUsersByFilter(search, pageable);

        assertThat(page.getTotalElements()).isEqualTo(total);
        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(1);
        final ExternalUserResponse first = page.getContent().getFirst();
        assertThat(first.username()).isEqualTo(username);
        assertThat(first.email()).isEqualTo(email);
        assertThat(first.name()).isEqualTo(firstName + " " + lastName);
        assertThat(first.externalAuthenticationPlatformId()).isEqualTo(id);
        final InOrder inOrder = inOrder(keycloakAdminApi);
        inOrder.verify(keycloakAdminApi).countUsers(REALM, search, false);
        inOrder.verify(keycloakAdminApi).listUsers(REALM, true, search, false, 0, 5);
        verifyNoMoreInteractions(keycloakAdminApi);
    }

    @Test
    void shouldReturnEmptyPageWhenNoUsers() {
        final String search = "search";
        final Pageable pageable = PageRequest.of(0, 20);
        when(keycloakAdminApi.countUsers(REALM, search, false)).thenReturn(0L);
        when(keycloakAdminApi.listUsers(REALM, true, search, false, 0, 20))
                .thenReturn(Collections.emptyList());

        final Page<ExternalUserResponse> page = service.listExternalUsersByFilter(search, pageable);

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @MethodSource("provideDataToTestFullNameCombinations")
    @ParameterizedTest()
    void shouldBuildFullNameWithNormalizationAndFallback(String firstName, String lastName, String expectedFullName, String username) {
        final String search = "search";
        final Pageable pageable = PageRequest.of(0, 10);
        when(keycloakAdminApi.countUsers(REALM, search, false)).thenReturn(1L);
        final var mapExternalResponse = new HashMap<String, Object>();
        final String email = "email";
        final String id = UUID.randomUUID().toString();
        mapExternalResponse.put("username", username);
        mapExternalResponse.put("email", email);
        mapExternalResponse.put("firstName", firstName);
        mapExternalResponse.put("lastName", lastName);
        mapExternalResponse.put("id", id);
        when(keycloakAdminApi.listUsers(REALM, true, search, false, 0, 10))
                .thenReturn(List.of(mapExternalResponse));

        final Page<ExternalUserResponse> page = service.listExternalUsersByFilter(search, pageable);

        assertThat(page.getContent()).hasSize(1);
        final ExternalUserResponse resp = page.getContent().getFirst();
        assertThat(resp.name()).isEqualTo(expectedFullName);
    }

    private static Stream<Arguments> provideDataToTestFullNameCombinations() {
        final String username = "username";
        final String firstName = "first";
        final String lastName = "last";
        final String fullName = firstName + " " + lastName;
        return Stream.of(
                Arguments.of(firstName, lastName, fullName, username),
                Arguments.of(firstName.concat("   "), lastName.concat("  "), fullName, username),
                Arguments.of(firstName.concat("  "), null, firstName, username),
                Arguments.of(null, lastName.concat("  "), lastName, username),
                Arguments.of(null, null, username, username),
                Arguments.of("   ", "\t", username, username),
                Arguments.of("   ", "\t", "", null)
        );
    }
}
