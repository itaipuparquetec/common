package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import br.org.itaipuparquetec.common.infrastructure.trail.envelope.UuidV7Generator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class OutboxRows {
   private final JdbcTemplate jdbc;
   private final Random random = new Random(5L);
   private Instant clock = Instant.parse("2026-10-07T13:45:12.000Z");

   OutboxRows(JdbcTemplate jdbc) {
      this.jdbc = jdbc;
   }

   List<UUID> insert(int count) {
      List<UUID> ids = new ArrayList();

      for(int number = 0; number < count; ++number) {
         ids.add(this.insertOne("{\"number\":" + number + "}"));
      }

      return ids;
   }

   UUID insertOne(String payload) {
      this.clock = this.clock.plusMillis(1L);
      UUID id = (new UuidV7Generator(Clock.fixed(this.clock, ZoneOffset.UTC), this.random)).next();
      this.jdbc.update("INSERT INTO audit_outbox (event_id, schema_version, tenant, source_service, trace_id, span_id, payload) VALUES (?, '1', 'acme_tenant', 'mirror', ?, ?, ?)", new Object[]{id, "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", payload});
      return id;
   }

   List<UUID> pendingIds() {
      return this.jdbc.queryForList("SELECT event_id FROM audit_outbox ORDER BY event_id", UUID.class);
   }

   int attemptsOf(UUID eventId) {
      return (Integer)this.jdbc.queryForObject("SELECT attempts FROM audit_outbox WHERE event_id = ?", Integer.class, new Object[]{eventId});
   }
}
