package br.org.itaipuparquetec.common.infrastructure.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class ReceiveKafkaMessage {

    public static final String BLACK_LIST_TOPIC = "BLACK_LIST_TOPIC";
    public static final String BLACK_LIST_RETRY_TOPIC = "BLACK_LIST_TOPIC";

    private final TokenBlacklist tokenBlacklist;
    private final KafkaTemplate<String, AuthenticationMessage> kafkaTemplate;

    @KafkaListener(topics = BLACK_LIST_TOPIC, autoStartup = "${kafka.blacklist-listener.enabled:false}")
    public void listenBlackListTopic(final AuthenticationMessage authenticationMessage) {
        try {

            log.info("Received message from BLACK_LIST_TOPIC");

            tokenBlacklist.revoke(authenticationMessage.token());

        } catch (Exception e) {
            log.error("Error processing blacklist message: {}", e.getMessage());
            kafkaTemplate.send(BLACK_LIST_RETRY_TOPIC, authenticationMessage);
        }
    }

    @KafkaListener(topics = BLACK_LIST_RETRY_TOPIC, autoStartup = "${kafka.blacklist-listener.enabled:false}")
    public void listenBlackListRetryTopic(final AuthenticationMessage authenticationMessage) {
        log.info("Received message from BLACK_LIST_RETRY_TOPIC");

        tokenBlacklist.revoke(authenticationMessage.token());
    }
}