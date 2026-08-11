package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantMigrationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TenantMigrationExceptionTest {

    @Test
    void keepsTheMessageAndCause() {
        final var cause = new IllegalStateException("root");
        final var exception = new TenantMigrationException("migration failed", cause);

        assertThat(exception).hasMessage("migration failed").hasCause(cause);
    }

    @Test
    void keepsTheMessageWithoutCause() {
        final var exception = new TenantMigrationException("migration failed");

        assertThat(exception).hasMessage("migration failed").hasNoCause();
    }
}
