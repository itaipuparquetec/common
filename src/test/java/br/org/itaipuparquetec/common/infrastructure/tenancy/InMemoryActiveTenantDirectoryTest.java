package br.org.itaipuparquetec.common.infrastructure.tenancy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryActiveTenantDirectoryTest {

    private final InMemoryActiveTenantDirectory directory = new InMemoryActiveTenantDirectory();

    @Test
    void theCentralTenantIsAlwaysActiveEvenBeforeTheLoad() {
        directory.deactivate("hubti");

        assertThat(directory.isActive("hubti")).isTrue();
    }

    @Test
    void aTenantIsActiveFromItsActivationUntilItsDeactivation() {
        assertThat(directory.isActive("acme_tenant")).isFalse();

        directory.activate("acme_tenant");
        assertThat(directory.isActive("acme_tenant")).isTrue();

        directory.deactivate("acme_tenant");
        assertThat(directory.isActive("acme_tenant")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void ignoresAnEventWithoutTheNameOfTheTenant(final String noName) {
        directory.activate(noName);
        directory.deactivate(noName);

        assertThat(directory.isActive(noName)).isFalse();
    }
}
