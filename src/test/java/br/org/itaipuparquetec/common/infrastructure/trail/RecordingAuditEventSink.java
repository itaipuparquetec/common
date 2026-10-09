package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import java.util.ArrayList;
import java.util.List;

public final class RecordingAuditEventSink implements AuditEventSink {
   private final List<AuditEvent> events = new ArrayList();
   private RuntimeException failureToThrow;

   public void publish(AuditEvent event) {
      this.events.add(event);
      if (this.failureToThrow != null) {
         throw this.failureToThrow;
      }
   }

   public void failWith(RuntimeException failure) {
      this.failureToThrow = failure;
   }

   public List<AuditEvent> events() {
      return this.events;
   }

   public AuditEvent onlyEvent() {
      if (this.events.size() != 1) {
         throw new IllegalStateException("Expected exactly one audit event but found " + this.events.size());
      } else {
         return (AuditEvent)this.events.get(0);
      }
   }
}
