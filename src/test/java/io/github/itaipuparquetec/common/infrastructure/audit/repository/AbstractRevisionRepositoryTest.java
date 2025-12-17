package io.github.itaipuparquetec.common.infrastructure.audit.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AbstractRevisionRepositoryTest {

    @Test
    void mustGetTypeOfRepository() {

        final var productRevisionRepository = new ProductRevisionRepository(null);

        assertThat(productRevisionRepository.clazz).isEqualTo(Produtct.class);
    }
}
