package br.org.itaipuparquetec.common.infrastructure.blacklist;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationMessageTest {

    @Test
    void tokenCanBeSetAndRead() {
        final var message = new AuthenticationMessage();
        message.token = "token123";

        final var token = message.token;

        assertThat(token).isEqualTo("token123");
    }
}

