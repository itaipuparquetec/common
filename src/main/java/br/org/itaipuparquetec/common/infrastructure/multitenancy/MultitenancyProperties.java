package br.org.itaipuparquetec.common.infrastructure.multitenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "hubti.multitenancy")
public record MultitenancyProperties(List<String> ignoredPaths) {

    public MultitenancyProperties {
        ignoredPaths = ignoredPaths == null ? List.of() : List.copyOf(ignoredPaths);
    }
}