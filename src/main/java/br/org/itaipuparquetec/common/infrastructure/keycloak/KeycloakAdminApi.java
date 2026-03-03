package br.org.itaipuparquetec.common.infrastructure.keycloak;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
public interface KeycloakAdminApi {

    @GetExchange("/admin/realms/{realm}/users")
    List<Map<String, Object>> listUsers(
            @PathVariable("realm") final String realm,
            @RequestParam("briefRepresentation") final boolean briefRepresentation,
            @RequestParam("search") final String search,
            @RequestParam("exact") final boolean exact,
            @RequestParam("first") final int first,
            @RequestParam("max") final int max
    );

    @GetExchange("/admin/realms/{realm}/users/count")
    Long countUsers(
            @PathVariable("realm") final String realm,
            @RequestParam("search") final String search,
            @RequestParam("exact") final boolean exact
    );
}
