package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import br.org.itaipuparquetec.common.infrastructure.audit.NotImplementedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class NotImplementedExceptionTest {

    @Test
    void mustCreateInstanceOfNotImplementedException() {
        final var exception = new NotImplementedException("Test message");

        assertThat(exception.getMessage()).isEqualTo("Test message");
    }
}
