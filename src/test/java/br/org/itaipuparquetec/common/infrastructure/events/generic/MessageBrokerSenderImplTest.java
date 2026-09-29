package br.org.itaipuparquetec.common.infrastructure.events.generic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageBrokerSenderImplTest {

    private static final String TOPIC = "ANY_TOPIC";
    private static final String MESSAGE = "any-message";

    private KafkaTemplate<String, String> kafkaTemplate;

    private MessageBrokerSenderImpl<String> sender;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        sender = new TestMessageBrokerSender(kafkaTemplate);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldPublishImmediatelyWhenThereIsNoActiveTransaction() {
        sender.send(TOPIC, MESSAGE);

        verify(kafkaTemplate).send(TOPIC, MESSAGE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotInvokeTheProvidedLoggerWhenThePublicationSucceeds() {
        final Consumer<String> logger = mock(Consumer.class);

        sender.send(TOPIC, MESSAGE, logger);

        verify(kafkaTemplate).send(TOPIC, MESSAGE);
        verifyNoInteractions(logger);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldInvokeTheProvidedLoggerWhenThePublicationFails() {
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("timeout")));
        final Consumer<String> logger = mock(Consumer.class);

        assertThatCode(() -> sender.send(TOPIC, MESSAGE, logger)).doesNotThrowAnyException();

        verify(logger).accept(MESSAGE);
    }

    @Test
    void shouldSwallowTheFailureWhenPublishingWithoutALoggerFails() {
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("timeout")));

        assertThatCode(() -> sender.send(TOPIC, MESSAGE)).doesNotThrowAnyException();
    }

    @Test
    void shouldPublishOnlyAfterTheCommitOfTheTransaction() {
        TransactionSynchronizationManager.initSynchronization();

        sender.send(TOPIC, MESSAGE);

        verifyNoInteractions(kafkaTemplate);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(kafkaTemplate).send(TOPIC, MESSAGE);
    }

    @Test
    void shouldNotPublishWhenTheTransactionIsRolledBack() {
        TransactionSynchronizationManager.initSynchronization();

        sender.send(TOPIC, MESSAGE);

        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldPropagateTheFailureWhenTheBrokerRefusesTheMessageSynchronously() {
        when(kafkaTemplate.send(anyString(), anyString())).thenThrow(new KafkaException("broker unavailable"));

        assertThatThrownBy(() -> sender.send(TOPIC, MESSAGE)).isInstanceOf(KafkaException.class);
    }

    private static final class TestMessageBrokerSender extends MessageBrokerSenderImpl<String> {
        private TestMessageBrokerSender(final KafkaTemplate<String, String> kafkaTemplate) {
            super(kafkaTemplate);
        }
    }
}
