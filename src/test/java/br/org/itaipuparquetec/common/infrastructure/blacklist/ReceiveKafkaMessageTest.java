package br.org.itaipuparquetec.common.infrastructure.blacklist;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReceiveKafkaMessageTest {

    private final TokenBlacklist tokenBlacklist = mock(TokenBlacklist.class);
    private final ReceiveKafkaMessage receiveKafkaMessage = new ReceiveKafkaMessage(tokenBlacklist);

    @Test
    void revokesTheTokenReceived() {
        receiveKafkaMessage.listenBlackListTopic(new AuthenticationMessage("token-value"));

        verify(tokenBlacklist).revoke("token-value");
    }

    @Test
    void everyInstanceReadsEveryRevokedTokenFromTheBeginningOfTheTopic() throws NoSuchMethodException {
        final var listener = ReceiveKafkaMessage.class.getMethod("listenBlackListTopic", AuthenticationMessage.class)
                .getAnnotation(KafkaListener.class);

        assertThat(listener.topics()).containsExactly(ReceiveKafkaMessage.BLACK_LIST_TOPIC);
        assertThat(listener.groupId()).contains("${random.uuid}");
        assertThat(listener.properties()).containsExactly("auto.offset.reset=earliest");
    }
}
