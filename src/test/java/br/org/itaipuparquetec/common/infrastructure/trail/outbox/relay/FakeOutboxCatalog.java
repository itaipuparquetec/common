package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.sql.DataSource;

final class FakeOutboxCatalog implements AuditOutboxCatalog {
   private final Map<String, DataSource> dataSources = new LinkedHashMap();

   FakeOutboxCatalog() {
   }

   FakeOutboxCatalog with(String tenant, DataSource dataSource) {
      this.dataSources.put(tenant, dataSource);
      return this;
   }

   public void forEachTenant(Consumer<String> action) {
      this.dataSources.keySet().forEach(action);
   }

   public DataSource dataSourceOf(String tenant) {
      return (DataSource)this.dataSources.get(tenant);
   }
}
