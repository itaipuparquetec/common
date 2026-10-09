package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Publishes outbox rows to the trail topic with a dedicated producer. The record key is the {@code traceId}, the
 * value is the envelope as is (JSON text) and the headers follow the event contract, including the W3C
 * {@code traceparent} of the step that produced the event.
 */
public class KafkaAuditOutboxPublisher implements AutoCloseable {

    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);
    private static final String SAMPLED_FLAG = "01";

    private final Producer<String, String> producer;
    private final String topic;

    public KafkaAuditOutboxPublisher(final Producer<String, String> producer, final String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    /**
     * Sends the row without waiting for the broker. A failure to even hand the record to the producer is returned
     * as an already failed future, so every row is handled the same way by the caller.
     *
     * @param row the outbox row
     * @return the pending acknowledgement
     */
    public Future<RecordMetadata> publish(final AuditOutboxRow row) {
        try {
            return producer.send(recordOf(row));
        } catch (final RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    @Override
    public void close() {
        producer.close(CLOSE_TIMEOUT);
    }

    private ProducerRecord<String, String> recordOf(final AuditOutboxRow row) {
        final List<Header> headers = List.of(
                header("trail-schema-version", row.schemaVersion()),
                header("trail-event-id", row.eventId().toString()),
                header("trail-tenant", row.tenant()),
                header("trail-source-service", row.sourceService()),
                header("traceparent", "00-" + row.traceId() + "-" + row.spanId() + "-" + SAMPLED_FLAG),
                header("content-type", "application/json"));
        return new ProducerRecord<>(topic, null, row.traceId(), row.payload(), headers);
    }

    private static Header header(final String name, final String value) {
        return new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
