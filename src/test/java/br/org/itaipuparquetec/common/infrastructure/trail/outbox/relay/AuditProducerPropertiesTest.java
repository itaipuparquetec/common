package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerializer;

class AuditProducerPropertiesTest {
   private static final Map<String, Object> SERVICE_PROPERTIES = Map.of("bootstrap.servers", "broker:9092", "security.protocol", "SSL", "acks", "1", "enable.idempotence", false, "value.serializer", JsonSerializer.class, "spring.json.add.type.headers", true, "spring.json.type.mapping", "authMessage:some.Type");

   AuditProducerPropertiesTest() {
   }

   @Test
   void shouldInheritTheConnectionAndSecuritySettingsOfTheService() {
      Map<String, Object> properties = AuditProducerProperties.from(SERVICE_PROPERTIES, "audit-relay-mirror");
      ((MapAssert)((MapAssert)Assertions.assertThat(properties).containsEntry("bootstrap.servers", "broker:9092")).containsEntry("security.protocol", "SSL")).containsEntry("client.id", "audit-relay-mirror");
   }

   @Test
   void shouldForceAcksAllAndIdempotenceWhateverTheServiceConfigured() {
      Map<String, Object> properties = AuditProducerProperties.from(SERVICE_PROPERTIES, "audit-relay-mirror");
      ((MapAssert)((MapAssert)((MapAssert)((MapAssert)((MapAssert)Assertions.assertThat(properties).containsEntry("acks", "all")).containsEntry("enable.idempotence", true)).containsEntry("retries", Integer.MAX_VALUE)).containsEntry("max.in.flight.requests.per.connection", 5)).containsEntry("delivery.timeout.ms", 120000)).containsEntry("compression.type", "lz4");
   }

   @Test
   void shouldUseStringSerializersForKeyAndValue() {
      Map<String, Object> properties = AuditProducerProperties.from(SERVICE_PROPERTIES, "audit-relay-mirror");
      ((MapAssert)Assertions.assertThat(properties).containsEntry("key.serializer", StringSerializer.class)).containsEntry("value.serializer", StringSerializer.class);
   }

   @Test
   void shouldDropTheSpringJsonTypeSettings() {
      Map<String, Object> properties = AuditProducerProperties.from(SERVICE_PROPERTIES, "audit-relay-mirror");
      Assertions.assertThat(properties.keySet()).noneMatch(key -> key.startsWith("spring.json."));
   }

   @Test
   void shouldNotChangeTheSettingsOfTheService() {
      AuditProducerProperties.from(SERVICE_PROPERTIES, "audit-relay-mirror");
      Assertions.assertThat(SERVICE_PROPERTIES).containsEntry("acks", "1");
   }
}
