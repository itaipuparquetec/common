package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import br.org.itaipuparquetec.common.infrastructure.audit.NotImplementedException;
import br.org.itaipuparquetec.common.infrastructure.audit.Revision;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractRevisionRepository<T> implements RevisionRepository<T> {

    final Class<T> clazz = resolveGenericType();

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * @param id       UUID
     * @param pageable Pageable
     * @return Page of RevisionDTO containing entity, revision metadata and changed properties
     */
    @Override
    public Page<RevisionDTO<T>> findRevisionsById(final UUID id, final Pageable pageable) {
        final AuditReader auditReader = getAuditReader();

        final Long total = (Long) auditReader.createQuery()
                .forRevisionsOfEntity(clazz, false, true)
                .add(AuditEntity.id().eq(id))
                .addProjection(AuditEntity.revisionNumber().count())
                .getSingleResult();

        @SuppressWarnings("unchecked") final List<Object[]> rawResults = auditReader.createQuery()
                .forRevisionsOfEntityWithChanges(clazz, false)
                .add(AuditEntity.id().eq(id))
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        final List<RevisionDTO<T>> content = rawResults.stream()
                .map(this::toRevisionDTO)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<RevisionDTO<T>> findRevisions(final Pageable pageable) {
        throw new NotImplementedException("You're probably the first to need it, so implement it for us...");
    }

    protected AuditReader getAuditReader() {
        return AuditReaderFactory.get(entityManager);
    }

    @SuppressWarnings("unchecked")
    private RevisionDTO<T> toRevisionDTO(final Object[] row) {
        final var dto = new RevisionDTO<T>();
        dto.setEntity((T) row[0]);

        final var revision = (Revision) row[1];
        dto.setRevisionId(revision.getId());
        dto.setDateTime(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(revision.getTimestamp()),
                ZoneId.systemDefault()
        ));
        dto.setExternalUserId(revision.getExternalUserId());
        dto.setType((RevisionType) row[2]);

        final Set<String> changedProps = (Set<String>) row[3];
        if (changedProps != null && !changedProps.isEmpty()) {
            dto.setChangedProps(String.join(", ", changedProps));
        }

        return dto;
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveGenericType() {
        final var type = getClass().getGenericSuperclass();
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }
}
