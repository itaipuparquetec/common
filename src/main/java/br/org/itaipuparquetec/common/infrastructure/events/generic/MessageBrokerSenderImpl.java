package br.org.itaipuparquetec.common.infrastructure.events.generic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

/**
 * Default implementation of the {@link MessageBrokerSender} interface to send messages to the message broker.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class MessageBrokerSenderImpl<M> implements MessageBrokerSender<M> {

    private final KafkaTemplate<String, M> kafkaTemplate;

    @Override
    public void send(final String topic, final M message) {
        send(topic, message, ignored -> {
        });
    }

    @Override
    public void send(final String topic, final M message, final Consumer<M> logger) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(topic, message, logger);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(topic, message, logger);
            }
        });
    }

    private void publish(final String topic, final M message, final Consumer<M> logger) {
        kafkaTemplate.send(topic, message)
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        log.atError()
                                .setCause(failure)
                                .log("Message could not be published");
                        logger.accept(message);
                    }
                });
    }
}
