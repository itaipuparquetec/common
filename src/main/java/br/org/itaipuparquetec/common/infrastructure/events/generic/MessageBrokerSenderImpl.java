package br.org.itaipuparquetec.common.infrastructure.events.generic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        afterTheCommit(() -> publish(() -> kafkaTemplate.send(topic, message), message, logger));
    }

    @Override
    public void send(final String topic, final String key, final M message, final Consumer<M> logger) {
        afterTheCommit(() -> publish(() -> kafkaTemplate.send(topic, key, message), message, logger));
    }

    private static void afterTheCommit(final Runnable publication) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publication.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publication.run();
            }
        });
    }

    private void publish(final Supplier<CompletableFuture<SendResult<String, M>>> sending,
                         final M message, final Consumer<M> logger) {
        sending.get()
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
