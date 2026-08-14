package br.org.itaipuparquetec.common.infrastructure.multitenancy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultitenancyPropertiesTest {

    @Test
    void bindsTheIgnoredPathsFromConfiguration() {
        final var source = new MapConfigurationPropertySource(Map.of(
                "hubti.multitenancy.ignored-paths[0]", "/public/**",
                "hubti.multitenancy.ignored-paths[1]", "/v1/accounts/authenticate"
        ));
        final var binder = new Binder(source);

        final var properties = binder.bind("hubti.multitenancy", MultitenancyProperties.class).get();

        assertThat(properties.ignoredPaths()).containsExactly("/public/**", "/v1/accounts/authenticate");
    }

    @Test
    void defaultsToAnEmptyListWhenNoPathsAreConfigured() {
        final var properties = new MultitenancyProperties(null);

        assertThat(properties.ignoredPaths()).isEmpty();
    }
}