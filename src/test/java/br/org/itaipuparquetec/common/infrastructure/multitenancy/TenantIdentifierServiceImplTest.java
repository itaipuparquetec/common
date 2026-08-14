package br.org.itaipuparquetec.common.infrastructure.multitenancy;

import br.org.itaipuparquetec.common.domain.exceptions.ForbiddenException;
import br.org.itaipuparquetec.common.domain.exceptions.NullFieldException;
import org.hibernate.cfg.MultiTenancySettings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class TenantIdentifierServiceImplTest {

    private final TenantIdentifierServiceImpl resolver = new TenantIdentifierServiceImpl();

    @Test
    void shouldNotSetNullWithTenantId() {
        assertThatThrownBy(() -> resolver.setTenantId(null)).isInstanceOf(NullFieldException.class)
                .hasMessage("The field \"tenantName\" cannot be null.");
    }

    @Test
    void resolvesTheTenantPreviouslySet() {
        resolver.setTenantId("tarcisio_tenant");

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("tarcisio_tenant");
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
    void customizeRegistersItselfAsTheTenantIdentifierResolver() {
        final Map<String, Object> properties = new HashMap<>();

        resolver.customize(properties);

        assertThat(properties.get(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER)).isSameAs(resolver);
    }

    @Test
    void doesNotValidateExistingCurrentSessions() {
        assertThat(resolver.validateExistingCurrentSessions()).isFalse();
    }

    @Test
    void onlyHubtiIsRoot() {
        assertThat(resolver.isRoot("hubti")).isTrue();
        assertThat(resolver.isRoot("tarcisio_tenant")).isFalse();
    }
}
