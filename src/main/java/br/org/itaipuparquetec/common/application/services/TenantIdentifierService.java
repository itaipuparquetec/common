package br.org.itaipuparquetec.common.application.services;

public interface TenantIdentifierService {

    void setTenantId(String id);

    void clear();

    String resolveCurrentTenantIdentifier();

    boolean isRoot(String tenant);
}
