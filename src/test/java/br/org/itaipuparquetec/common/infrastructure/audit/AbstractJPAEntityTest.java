package br.org.itaipuparquetec.common.infrastructure.audit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AbstractJPAEntityTest {

    @Test
    void mustCreateInstanceOfAbstractJPAEntity() {
        final var abstractJPAEntity = new AbstractJPAEntity() {
        };

        Assertions.assertThat(abstractJPAEntity).isNotNull();
        Assertions.assertThat(abstractJPAEntity.getId()).isNull();
    }
}
