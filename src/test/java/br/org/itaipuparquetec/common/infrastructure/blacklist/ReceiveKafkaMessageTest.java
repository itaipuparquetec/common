package br.org.itaipuparquetec.common.infrastructure.blacklist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReceiveKafkaMessageTest {

    private final TokenBlacklist tokenBlacklist = mock(TokenBlacklist.class);
    private final KafkaTemplate<String, AuthenticationMessage> kafkaTemplate = mock(KafkaTemplate.class);

    private ReceiveKafkaMessage receiveKafkaMessage;

    @BeforeEach
    void setUp() {
        receiveKafkaMessage = new ReceiveKafkaMessage(tokenBlacklist, kafkaTemplate);
    }

    @Test
    void revokeCalledWhenMessageReceived() {
        final var message = new AuthenticationMessage("token-value");

        receiveKafkaMessage.listenBlackListTopic(message);

        verify(tokenBlacklist).revoke("token-value");
    }

    @Test
    void handlesExceptionFromRevoke() {
        final var message = new AuthenticationMessage("bad-token");
        doThrow(new RuntimeException("boom")).when(tokenBlacklist).revoke("bad-token");

        assertThatCode(() -> receiveKafkaMessage.listenBlackListTopic(message)).doesNotThrowAnyException();

        verify(tokenBlacklist).revoke("bad-token");
    }

    @Test
    void revokeCalledWhenRetryMessageReceived() {
        final var message = new AuthenticationMessage("retry-token");

        receiveKafkaMessage.listenBlackListRetryTopic(message);

        verify(tokenBlacklist).revoke("retry-token");
    }

    @Test
    void propagatesExceptionWhenRetryRevokeFails() {
        final var message = new AuthenticationMessage("bad-retry-token");
        doThrow(new RuntimeException("retry boom")).when(tokenBlacklist).revoke("bad-retry-token");

        assertThatThrownBy(() -> receiveKafkaMessage.listenBlackListRetryTopic(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("retry boom");
    }
}

