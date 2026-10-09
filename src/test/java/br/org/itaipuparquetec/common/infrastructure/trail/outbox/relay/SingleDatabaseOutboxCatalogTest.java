package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import java.util.ArrayList;
import java.util.Objects;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SingleDatabaseOutboxCatalogTest {
   private final DataSource dataSource = (DataSource)Mockito.mock(DataSource.class);
   private final SingleDatabaseOutboxCatalog catalog;

   SingleDatabaseOutboxCatalogTest() {
      this.catalog = new SingleDatabaseOutboxCatalog("hubti", this.dataSource);
   }

   @Test
   void shouldVisitOnlyTheSingleTenant() {
      ArrayList<String> visited = new ArrayList();
      SingleDatabaseOutboxCatalog var10000 = this.catalog;
      Objects.requireNonNull(visited);
      var10000.forEachTenant(visited::add);
      Assertions.assertThat(visited).containsExactly(new String[]{"hubti"});
   }

   @Test
   void shouldAlwaysResolveTheSingleDataSource() {
      DataSource resolved = this.catalog.dataSourceOf("any_tenant");
      Assertions.assertThat(resolved).isSameAs(this.dataSource);
   }
}
