package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

/**
 * DTO com as informações para a criação do DataSource.
 *
 * @param jdbcUrl  {@link String}
 * @param username {@link String}
 * @param password {@link String}
 */
public record TenantConnectionInfo(String jdbcUrl, String username, String password) {
}
