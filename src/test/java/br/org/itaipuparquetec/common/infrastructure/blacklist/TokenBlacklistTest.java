package br.org.itaipuparquetec.common.infrastructure.blacklist;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistTest {

    @Test
    void isRevokedReturnsFalseForUnknownToken() {
        final var blacklist = new TokenBlacklist();

        final var result = blacklist.isRevoked("no-such-token");

        assertThat(result).isFalse();
    }

    @Test
    void revokeMakesTokenRevoked() {
        final var blacklist = new TokenBlacklist();

        blacklist.revoke("abc-token");

        assertThat(blacklist.isRevoked("abc-token")).isTrue();
    }
}

