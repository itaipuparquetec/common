package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer settings of the audit relay. Connection and security settings (bootstrap servers, SSL, SASL) are
 * inherited from the service Kafka configuration, but everything that shapes delivery is fixed here: {@code acks=all},
 * idempotence, {@code String} key and value, and no Spring type headers.
 */
public final class AuditProducerProperties {

    private static final String SPRING_JSON_PREFIX = "spring.json.";
    private static final int DELIVERY_TIMEOUT_MILLIS = 120_000;
    private static final int REQUEST_TIMEOUT_MILLIS = 30_000;
    private static final int MAX_BLOCK_MILLIS = 5_000;
    private static final int LINGER_MILLIS = 5;
    private static final int MAX_IN_FLIGHT_REQUESTS = 5;

    private AuditProducerProperties() {
    }

    public static Map<String, Object> from(final Map<String, Object> serviceProducerProperties,
                                           final String clientId) {
        final Map<String, Object> properties = new HashMap<>(serviceProducerProperties);
        properties.keySet().removeIf(key -> key.startsWith(SPRING_JSON_PREFIX));
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, MAX_IN_FLIGHT_REQUESTS);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, DELIVERY_TIMEOUT_MILLIS);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, REQUEST_TIMEOUT_MILLIS);
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, MAX_BLOCK_MILLIS);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MILLIS);
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return properties;
    }
}
