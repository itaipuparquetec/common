package br.org.itaipuparquetec.common.infrastructure.trail.identity;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;
import java.util.ArrayList;
import java.util.List;

public final class RecordingTenantIdentifierService implements TenantIdentifierService {
   private final List<String> calls = new ArrayList();
   private String current = "hubti";

   public void setTenantId(String id) {
      this.calls.add("set:" + id);
      this.current = id;
   }

   public void clear() {
      this.calls.add("clear");
      this.current = "hubti";
   }

   public String resolveCurrentTenantIdentifier() {
      return this.current;
   }

   public boolean isRoot(String tenant) {
      return "hubti".equals(tenant);
   }

   public List<String> calls() {
      return this.calls;
   }
}
