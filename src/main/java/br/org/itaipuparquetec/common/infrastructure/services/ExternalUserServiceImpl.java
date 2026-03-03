package br.org.itaipuparquetec.common.infrastructure.services;

import br.org.itaipuparquetec.common.application.services.ExternalUserService;
import br.org.itaipuparquetec.common.infrastructure.keycloak.KeycloakAdminApi;
import br.org.itaipuparquetec.common.infrastructure.keycloak.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cristian.souza
 * @version 1.0.0
 * @since 14/10/2025
 */
@Service
@RequiredArgsConstructor
public class ExternalUserServiceImpl implements ExternalUserService {

    private final KeycloakAdminApi keycloakAdminApi;
    private final KeycloakProperties keycloakProperties;

    @Override
    public Page<ExternalUserResponse> listExternalUsersByFilter(final String search, final Pageable pageable) {
        final long total = keycloakAdminApi.countUsers(keycloakProperties.realm(), search, false);

        final List<Map<String, Object>> rawUsers = keycloakAdminApi.listUsers(
                keycloakProperties.realm(),
                true,
                search,
                false,
                (int) pageable.getOffset(),
                pageable.getPageSize()
        );

        final List<ExternalUserResponse> items = rawUsers.stream()
                .map(this::toDto)
                .toList();

        return new PageImpl<>(items, pageable, total);
    }

    private ExternalUserResponse toDto(final Map<String, Object> raw) {
        final String username = this.asString(raw.get("username"));
        final String email = this.asString(raw.get("email"));
        final String firstName = this.asString(raw.get("firstName"));
        final String lastName = this.asString(raw.get("lastName"));
        final String id = this.asString(raw.get("id"));

        String fullName = (nullToEmpty(firstName) + " " + nullToEmpty(lastName))
                .trim().replaceAll("\\s+", " ");

        if (fullName.isBlank()) {
            fullName = username != null ? username : "";
        }

        return new ExternalUserResponse(
                username,
                email,
                fullName,
                id
        );
    }

    private String asString(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
