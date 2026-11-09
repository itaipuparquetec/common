package br.org.itaipuparquetec.common.infrastructure.blacklist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Keeps the {@link TokenBlacklist} of this instance with the revoked tokens the authorization server publishes. Every
 * instance of every service needs every revoked token, so each instance has a consumer group of its own. It reads
 * the topic from its beginning: a new instance rebuilds the tokens still revoked, as long as the retention of the
 * topic covers the longest life of an access token.
 */
@Slf4j
public class ReceiveKafkaMessage {

    public static final String BLACK_LIST_TOPIC = "BLACK_LIST_TOPIC";
    private static final String GROUP_OF_THE_INSTANCE = "${spring.application.name:hubti}-blacklist-${random.uuid}";
    private static final String FROM_THE_BEGINNING = "auto.offset.reset=earliest";

    private final TokenBlacklist tokenBlacklist;

    public ReceiveKafkaMessage(final TokenBlacklist tokenBlacklist) {
        this.tokenBlacklist = tokenBlacklist;
    }

    @KafkaListener(topics = BLACK_LIST_TOPIC, groupId = GROUP_OF_THE_INSTANCE, properties = FROM_THE_BEGINNING,
            autoStartup = "${kafka.blacklist-listener.enabled:false}")
    public void listenBlackListTopic(final AuthenticationMessage authenticationMessage) {
        log.debug("Received a revoked token from {}", BLACK_LIST_TOPIC);
        tokenBlacklist.revoke(authenticationMessage.token());
    }
}
