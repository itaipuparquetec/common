package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RevisionRepository<T> {

    Page<RevisionDTO<T>> findRevisionsById(UUID id, Pageable pageable);

    Page<RevisionDTO<T>> findRevisions(Pageable pageable);

}
