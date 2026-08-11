package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantConnectionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TenantConnectionExceptionTest {

    @Test
    void keepsTheMessage() {
        final var exception = new TenantConnectionException("cannot connect");

        assertThat(exception).hasMessage("cannot connect").hasNoCause();
    }

    @Test
    void keepsTheMessageAndCause() {
        final var cause = new IllegalStateException("root");
        final var exception = new TenantConnectionException("cannot connect", cause);

        assertThat(exception).hasMessage("cannot connect").hasCause(cause);
    }
}
