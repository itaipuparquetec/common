package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import java.util.ArrayList;
import java.util.List;

public final class FakeAuditOutboxStore implements AuditOutboxStore {
   private final List<AuditOutboxEntry> entries = new ArrayList();
   private RuntimeException failureToThrow;

   public FakeAuditOutboxStore failingWith(RuntimeException failure) {
      this.failureToThrow = failure;
      return this;
   }

   public void add(AuditOutboxEntry entry) {
      if (this.failureToThrow != null) {
         throw this.failureToThrow;
      } else {
         this.entries.add(entry);
      }
   }

   public List<AuditOutboxEntry> entries() {
      return this.entries;
   }
}
