package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import br.org.itaipuparquetec.common.infrastructure.audit.NotImplementedException;
import br.org.itaipuparquetec.common.infrastructure.audit.Revision;
import org.assertj.core.api.Assertions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AbstractRevisionRepositoryTest {

    private AuditReader auditReader;
    private AuditQuery countQuery;
    private AuditQuery contentQuery;

    @BeforeEach
    void setUp() {
        auditReader = mock(AuditReader.class);
        final var countQueryCreator = mock(AuditQueryCreator.class);
        final var contentQueryCreator = mock(AuditQueryCreator.class);
        countQuery = mock(AuditQuery.class);
        contentQuery = mock(AuditQuery.class);
        when(auditReader.createQuery()).thenReturn(countQueryCreator).thenReturn(contentQueryCreator);
        when(countQueryCreator.forRevisionsOfEntity(Item.class, false, true)).thenReturn(countQuery);
        when(countQuery.add(any())).thenReturn(countQuery);
        when(countQuery.addProjection(any())).thenReturn(countQuery);
        when(contentQueryCreator.forRevisionsOfEntityWithChanges(Item.class, false)).thenReturn(contentQuery);
        when(contentQuery.add(any())).thenReturn(contentQuery);
        when(contentQuery.setFirstResult(anyInt())).thenReturn(contentQuery);
        when(contentQuery.setMaxResults(anyInt())).thenReturn(contentQuery);
    }

    @Test
    void shouldReturnClassOfRepository() {
        var repo = new ItemRevisionRepository();
        assertThat(repo.clazz).isEqualTo(Item.class);
    }

    @Test
    void shouldReturnPageOfRevisionsById() {
        UUID id = UUID.randomUUID();
        final var pageable = PageRequest.of(0, 10);
        var revision = mockRevision(1L, "john");
        var entity = new Item();
        Object[] row = mockRow(entity, revision, RevisionType.ADD, Set.of("name", "group"));
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(contentQuery.getResultList()).thenReturn(rows);

        var repo = new ItemRevisionRepository(auditReader);
        var result = repo.findRevisionsById(id, pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        var dto = result.getContent().getFirst();
        assertThat(dto.getRevisionId()).isEqualTo(1L);
        assertThat(dto.getExternalUserId()).isEqualTo("john");
        assertThat(dto.getType()).isEqualTo(RevisionType.ADD);
        assertThat(dto.getChangedProps()).isNotNull();
        assertThat(dto.getDateTime()).isNotNull();
    }

    @Test
    void shouldReturnNullChangedPropsWhenSetIsEmpty() {
        final var id = UUID.randomUUID();
        final var pageable = PageRequest.of(0, 10);
        var revision = mockRevision(2L, "jane");
        var entity = new Item();
        Object[] row = mockRow(entity, revision, RevisionType.MOD, Set.of());
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(contentQuery.getResultList()).thenReturn(rows);

        var repo = new ItemRevisionRepository(auditReader);
        var result = repo.findRevisionsById(id, pageable);

        var dto = result.getContent().getFirst();
        assertThat(dto.getChangedProps()).isNull();
    }

    @Test
    void shouldReturnEmptyPageWhenNoRevisionsFound() {
        final var id = UUID.randomUUID();
        final var pageable = PageRequest.of(0, 10);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(contentQuery.getResultList()).thenReturn(Collections.emptyList());

        var repo = new ItemRevisionRepository(auditReader);
        var result = repo.findRevisionsById(id, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void mustThrowAnExceptionWhenFindRevisions() {
        final var productRevisionRepository = new ItemRevisionRepository();
        final var exception = assertThrows(NotImplementedException.class,
                () -> productRevisionRepository.findRevisions(null));
        Assertions.assertThat(exception.getMessage())
                .contains("You're probably the first to need it, so implement it for us...");
    }

    private Revision mockRevision(Long id, String user) {
        var revision = mock(Revision.class);
        when(revision.getId()).thenReturn(id);
        when(revision.getTimestamp()).thenReturn(1700000000000L);
        when(revision.getExternalUserId()).thenReturn(user);
        return revision;
    }

    private Object[] mockRow(Item entity, Revision revision, RevisionType type, Set<String> changedProps) {
        return new Object[]{entity, revision, type, changedProps};
    }
}