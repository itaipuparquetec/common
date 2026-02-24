package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractRevisionRepositoryTest {

    @Test
    void mustGetTypeOfRepository() {

        final var productRevisionRepository = new ProductRevisionRepository();

        assertThat(productRevisionRepository.clazz).isEqualTo(Produtct.class);
    }

    @Test
    void mustThrowAnExceptionWhenFindRevisionsById() {
        final var productRevisionRepository = new ProductRevisionRepository();

        final var exception = assertThrows(NotImplementedException.class,
                () -> productRevisionRepository.findRevisionsById(null, null));

        Assertions.assertThat(exception.getMessage())
                .contains("You're probably the first to need it, so implement it for us...");
    }

    @Test
    void mustThrowAnExceptionWhenFindRevisions() {
        final var productRevisionRepository = new ProductRevisionRepository();

        final var exception = assertThrows(NotImplementedException.class,
                () -> productRevisionRepository.findRevisions(null));

        Assertions.assertThat(exception.getMessage())
                .contains("You're probably the first to need it, so implement it for us...");
    }
}
