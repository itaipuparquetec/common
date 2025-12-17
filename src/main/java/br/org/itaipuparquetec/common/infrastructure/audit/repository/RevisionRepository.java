package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RevisionRepository<T> {

    Page<T> findRevisionsById(final UUID id, Pageable pageable);

    Page<T> findRevisions(Pageable pageable);

}
