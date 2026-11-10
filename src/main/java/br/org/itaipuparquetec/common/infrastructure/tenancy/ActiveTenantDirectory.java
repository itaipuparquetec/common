package br.org.itaipuparquetec.common.infrastructure.tenancy;

/**
 * Tells whether a tenant exists and is active (RF 3.2.2 of Tenants): the tokens of an inactive tenant are refused,
 * even within their validity (RSF 3.1.7.1).
 */
@FunctionalInterface
public interface ActiveTenantDirectory {

    boolean isActive(String tenantName);
}
