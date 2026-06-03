package br.org.itaipuparquetec.common.infrastructure.blacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReceiveKafkaMessageTest {

    private TokenBlacklist tokenBlacklist;
    private ReceiveKafkaMessage receiver;

    @BeforeEach
    void setUp() {
        tokenBlacklist = mock(TokenBlacklist.class);
        receiver = new ReceiveKafkaMessage(tokenBlacklist);
    }

    @Test
    void revokeCalledWhenMessageReceived() {
        final var message = new AuthenticationMessage();
        message.token = "token-value";

        receiver.listenBlackListTopic(message);

        verify(tokenBlacklist).revoke("token-value");
    }

    @Test
    void handlesExceptionFromRevoke() {
        final var message = new AuthenticationMessage();
        message.token = "bad-token";
        doThrow(new RuntimeException("boom")).when(tokenBlacklist).revoke("bad-token");

        assertThatCode(() -> receiver.listenBlackListTopic(message)).doesNotThrowAnyException();

        verify(tokenBlacklist).revoke("bad-token");
    }
}

