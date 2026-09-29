package br.org.itaipuparquetec.common.infrastructure.events.generic;

import java.util.function.Consumer;

/**
 * Generic Interface to send messages to the message broker.
 *
 * @param <M>
 */
public interface MessageBrokerSender<M> {
    /**
     * Sends a message to the message broker.
     *
     * @param topic   {@link String} The topic to send the message.
     * @param message {@link M} The message to send.
     */
    void send(String topic, M message);

    /**
     * Sends a message to the message broker.
     *
     * @param topic   {@link String} The topic to send the message.
     * @param message {@link M} The message to send.
     * @param logger  {@link Consumer} The logger to log the exceptions throwed by integration.
     */
    void send(String topic, M message, Consumer<M> logger);
}
