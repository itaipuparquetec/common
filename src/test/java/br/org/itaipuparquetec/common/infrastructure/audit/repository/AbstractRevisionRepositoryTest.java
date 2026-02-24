package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AbstractRevisionRepositoryTest {

    @Test
    void mustGetTypeOfRepository() {

        final var productRevisionRepository = new ProductRevisionRepository();

        assertThat(productRevisionRepository.clazz).isEqualTo(Produtct.class);
    }
}
