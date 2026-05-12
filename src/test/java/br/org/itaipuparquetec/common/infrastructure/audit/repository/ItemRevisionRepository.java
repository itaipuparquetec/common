package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import org.hibernate.envers.AuditReader;

public class ItemRevisionRepository extends AbstractRevisionRepository<Item> {

    private final AuditReader auditReader;

    protected ItemRevisionRepository() {
        this.auditReader = null;
    }

    ItemRevisionRepository(final AuditReader auditReader) {
        this.auditReader = auditReader;
    }

    @Override
    protected AuditReader getAuditReader() {
        return auditReader != null ? auditReader : super.getAuditReader();
    }
}
