package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditError;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditErrorCategory;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditResult;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditActorType;
import java.time.Instant;
import java.util.UUID;

public final class AuditEventFixture {
   public static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
   public static final String SPAN_ID = "00f067aa0ba902b7";
   public static final String PARENT_SPAN_ID = "a3ce929d0e0e4736";
   public static final Instant OCCURRED_AT = Instant.parse("2026-10-07T13:45:12.345678Z");
   public static final UUID EVENT_ID = UUID.fromString("01929f3c-7a1e-7c2b-9d4e-2f6a8b0c1d23");

   private AuditEventFixture() {
   }

   public static AuditEvent successOf(String useCase) {
      return successWithInput(useCase, (Object)null);
   }

   public static AuditEvent successWithInput(String useCase, Object input) {
      return new AuditEvent(EVENT_ID, OCCURRED_AT, 42L, "acme_tenant", "acme", "mirror", "0.8.0", "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", "a3ce929d0e0e4736", useCase, "1", "user-1", AuditActorType.USER, "sid-1", "jti-1", AuditResult.SUCCESS, (AuditError)null, input);
   }

   public static AuditEvent failureOf(String useCase) {
      AuditError error = new AuditError("ItemAlreadyExistsException", AuditErrorCategory.BUSINESS);
      return new AuditEvent(EVENT_ID, OCCURRED_AT, 7L, "acme_tenant", (String)null, "mirror", (String)null, "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", (String)null, useCase, "1", (String)null, AuditActorType.SYSTEM, (String)null, (String)null, AuditResult.BUSINESS_ERROR, error, (Object)null);
   }
}
