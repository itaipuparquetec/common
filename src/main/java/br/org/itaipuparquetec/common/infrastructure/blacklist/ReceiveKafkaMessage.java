package br.org.itaipuparquetec.common.infrastructure.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@RequiredArgsConstructor
public class ReceiveKafkaMessage {

    public static final String BLACK_LIST_TOPIC = "BLACK_LIST_TOPIC";

    private final TokenBlacklist tokenBlacklist;

    @KafkaListener(topics = BLACK_LIST_TOPIC)
    public void listenBlackListTopic(AuthenticationMessage authenticationMessage) {
        try {

            log.info("Received message from BLACK_LIST_TOPIC");

            tokenBlacklist.revoke(authenticationMessage.token);

        } catch (Exception e) {
            log.error("Error processing blacklist message: {}", e.getMessage());
        }
    }
}