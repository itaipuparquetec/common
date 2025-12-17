package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.UUID;

/**
 * AbstractService to converting audit objects to legible objects
 *
 * @param <T>
 */
public abstract class AbstractRevisionRepository<T> implements RevisionRepository<T> {

    /**
     *
     */
    private AuditReader reader;

    /**
     *
     */
    private final EntityManagerFactory entityManagerFactory;

    /**
     *
     */
    final Class<T> clazz = resolveGenericType();

    protected AbstractRevisionRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     *
     */
    @PostConstruct
    public void postConstruct() {
        reader = AuditReaderFactory.get(Objects.requireNonNull(entityManagerFactory.createEntityManager()));
    }

    /**
     * @param id Long
     * @return Revision
     */
    @Override
    public Page<T> findRevisionsById(final UUID id, final Pageable pageable) {
        throw new NotImplementedException("You're probably the first to need it, so implement it for us...");
    }

    @Override
    public Page<T> findRevisions(Pageable pageable) {
        throw new NotImplementedException("You're probably the first to need it, so implement it for us...");
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveGenericType() {
        final var type = getClass().getGenericSuperclass();
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }
}
