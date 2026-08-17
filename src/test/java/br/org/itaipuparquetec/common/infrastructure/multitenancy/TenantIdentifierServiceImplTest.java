package br.org.itaipuparquetec.common.infrastructure.multitenancy;

import br.org.itaipuparquetec.common.domain.exceptions.EmptyFieldException;
import br.org.itaipuparquetec.common.domain.exceptions.NullFieldException;
import org.hibernate.cfg.MultiTenancySettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class TenantIdentifierServiceImplTest {

    private final TenantIdentifierServiceImpl resolver = new TenantIdentifierServiceImpl();

    @Test
    void shouldNotSetNullWithTenantId() {
        assertThatThrownBy(() -> resolver.setTenantId(null)).isInstanceOf(NullFieldException.class)
                .hasMessage("The field \"tenantName\" cannot be null.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void shouldNotSetEmptyOrBlankTenantId(final String blankTenant) {
        assertThatThrownBy(() -> resolver.setTenantId(blankTenant)).isInstanceOf(EmptyFieldException.class)
                .hasMessage("The field \"tenantName\" cannot be empty.");
    }

    @ParameterizedTest
    @CsvSource({
            "tarcisio,       tarcisio_tenant",
            "acme,           acme_tenant",
            "tarcisio_tenant, tarcisio_tenant",
            "acme_tenant,    acme_tenant",
            "hubti,          hubti"
    })
    void resolvesTheStoredTenantAppendingTheSuffixOnlyWhenNeeded(final String tenantName, final String expected) {
        resolver.setTenantId(tenantName);

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(expected);
    }

    @Test
    void resolvesTheTenantPreviouslySet() {
        resolver.setTenantId("tarcisio_tenant");

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("tarcisio_tenant");
    }

    @Test
    void appendsSuffixWhenTenantNameHasNoSuffix() {
        resolver.setTenantId("tarcisio");

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("tarcisio_tenant");
    }

    @Test
    void storesHubtiWithoutAppendingSuffix() {
        resolver.setTenantId("hubti");

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("hubti");
    }

    @Test
    void fallsBackToHubtiWhenNoTenantIsSet() {
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("hubti");
    }

    @Test
    void clearRemovesThePreviouslySetTenant() {
        resolver.setTenantId("tarcisio_tenant");

        resolver.clear();

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("hubti");
    }

    @Test
    void clearWhenNothingWasSetKeepsFallback() {
        resolver.clear();

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("hubti");
    }

    @Test
    void keepsTheTenantIsolatedPerThread() throws InterruptedException {
        resolver.setTenantId("tarcisio_tenant");
        final var tenantSeenByOtherThread = new AtomicReference<String>();

        final var otherThread = new Thread(() -> tenantSeenByOtherThread.set(resolver.resolveCurrentTenantIdentifier()));
        otherThread.start();
        otherThread.join();

        assertThat(tenantSeenByOtherThread.get()).isEqualTo("hubti");
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("tarcisio_tenant");
    }

    @Test
    void customizeRegistersItselfAsTheTenantIdentifierResolver() {
        final Map<String, Object> properties = new HashMap<>();

        resolver.customize(properties);

        assertThat(properties.get(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER)).isSameAs(resolver);
    }

    @Test
    void doesNotValidateExistingCurrentSessions() {
        assertThat(resolver.validateExistingCurrentSessions()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "hubti,          true",
            "tarcisio_tenant, false",
            "hubti_tenant,   false"
    })
    void onlyExactHubtiIsRoot(final String tenant, final boolean expected) {
        assertThat(resolver.isRoot(tenant)).isEqualTo(expected);
    }

    @Test
    void isRootReturnsFalseForNull() {
        assertThat(resolver.isRoot(null)).isFalse();
    }
}
