package br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.exceptions.TenantConnectionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantConnectionInfoProviderTest {

    private static final String USERNAME = "hubti_user";
    private static final String PASSWORD = "secret";

    @Test
    void resolvesJdbcUrlSwappingDatabaseSegmentForTheTenant() {
        final TenantConnectionInfoProvider provider = new TenantConnectionInfoProvider(
                "jdbc:postgresql://localhost:5432/hubti", USERNAME, PASSWORD);

        final TenantConnectionInfo info = provider.resolveInfoFor("tarcisio_tenant");

        assertThat(info.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/tarcisio_tenant");
        assertThat(info.username()).isEqualTo(USERNAME);
        assertThat(info.password()).isEqualTo(PASSWORD);
    }

    @Test
    void failsWhenCentralUrlHasNoDatabaseSegment() {
        final TenantConnectionInfoProvider provider = new TenantConnectionInfoProvider(
                "jdbc:postgresql:hubti", USERNAME, PASSWORD);

        assertThatThrownBy(() -> provider.resolveInfoFor("tarcisio_tenant"))
                .isInstanceOf(TenantConnectionException.class)
                .hasMessageContaining("jdbc:postgresql:hubti");
    }
}
