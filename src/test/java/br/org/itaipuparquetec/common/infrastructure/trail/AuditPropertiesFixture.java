package br.org.itaipuparquetec.common.infrastructure.trail;

import java.time.Duration;

public final class AuditPropertiesFixture {
   private AuditPropertiesFixture() {
   }

   public static AuditProperties withCleartext(boolean publicData, boolean internal, boolean confidential) {
      return new AuditProperties(true, "salt", new AuditProperties.Cleartext(publicData, internal, confidential), AuditSinkType.LOG, false, 32768, (String)null, topics(), new AuditProperties.Outbox(true, 2, 100), relay());
   }

   public static AuditProperties withSink(AuditSinkType sink, boolean includeReadOnly, int maxInputBytes) {
      return new AuditProperties(true, "salt", new AuditProperties.Cleartext(true, true, false), sink, includeReadOnly, maxInputBytes, "9.9.9", topics(), new AuditProperties.Outbox(true, 2, 100), relay());
   }

   public static AuditProperties.Relay relay() {
      return new AuditProperties.Relay(true, Duration.ofMillis(50L), 10, Duration.ofSeconds(2L), Duration.ofSeconds(60L), 3);
   }

   private static AuditProperties.Topics topics() {
      return new AuditProperties.Topics("hubti.trail.events", "hubti.trail.events.dlt");
   }
}
