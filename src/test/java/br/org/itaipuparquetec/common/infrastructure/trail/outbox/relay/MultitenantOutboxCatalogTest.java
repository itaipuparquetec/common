package br.org.itaipuparquetec.common.infrastructure.trail.outbox.relay;

import br.org.itaipuparquetec.common.infrastructure.multitenancy.datasource.TenantDataSourceRegistryImpl;
import br.org.itaipuparquetec.common.infrastructure.multitenancy.providers.PostgreSQLMigrationServiceImpl;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class MultitenantOutboxCatalogTest {
   private final FakeTenantDiscovery discovery = new FakeTenantDiscovery();
   private final FakeRegistry registry = new FakeRegistry();
   private final MutableClock clock = new MutableClock();
   private final MultitenantOutboxCatalog catalog;

   MultitenantOutboxCatalogTest() {
      this.catalog = new MultitenantOutboxCatalog(this.discovery, this.registry, this.clock, Duration.ofSeconds(60L));
   }

   @Test
   void shouldVisitEveryTenantFoundByTheDiscovery() {
      this.discovery.tenants = Set.of("hubti", "acme_tenant");
      List<String> visited = this.visitedTenants();
      Assertions.assertThat(visited).containsExactlyInAnyOrder(new String[]{"hubti", "acme_tenant"});
   }

   @Test
   void shouldNotAskTheDiscoveryAgainBeforeTheRefreshInterval() {
      this.discovery.tenants = Set.of("hubti");
      this.visitedTenants();
      this.discovery.tenants = Set.of("hubti", "new_tenant");
      this.clock.advance(Duration.ofSeconds(59L));
      List<String> visited = this.visitedTenants();
      Assertions.assertThat(visited).containsExactly(new String[]{"hubti"});
      Assertions.assertThat(this.discovery.calls).isEqualTo(1);
   }

   @Test
   void shouldPickUpNewTenantsAfterTheRefreshInterval() {
      this.discovery.tenants = Set.of("hubti");
      this.visitedTenants();
      this.discovery.tenants = Set.of("hubti", "new_tenant");
      this.clock.advance(Duration.ofSeconds(60L));
      List<String> visited = this.visitedTenants();
      Assertions.assertThat(visited).containsExactlyInAnyOrder(new String[]{"hubti", "new_tenant"});
   }

   @Test
   void shouldKeepTheLastKnownTenantsWhenARefreshFails() {
      this.discovery.tenants = Set.of("hubti");
      this.visitedTenants();
      this.discovery.failing = true;
      this.clock.advance(Duration.ofSeconds(61L));
      List<String> visited = this.visitedTenants();
      Assertions.assertThat(visited).containsExactly(new String[]{"hubti"});
   }

   @Test
   void shouldVisitNothingWhenTheFirstDiscoveryFails() {
      this.discovery.failing = true;
      List<String> visited = this.visitedTenants();
      Assertions.assertThat(visited).isEmpty();
   }

   @Test
   void shouldResolveTheDataSourceOfTheTenantThroughTheRegistry() {
      DataSource dataSource = this.registry.dataSource;
      DataSource resolved = this.catalog.dataSourceOf("acme_tenant");
      Assertions.assertThat(resolved).isSameAs(dataSource);
      Assertions.assertThat(this.registry.requestedTenant).isEqualTo("acme_tenant");
   }

   private List<String> visitedTenants() {
      List<String> visited = new ArrayList<>();
       Objects.requireNonNull(visited);
      this.catalog.forEachTenant(visited::add);
      return visited;
   }

   private static final class FakeTenantDiscovery extends PostgreSQLMigrationServiceImpl {
      private Set<String> tenants = new HashSet<>();
      private boolean failing;
      private int calls;

      private FakeTenantDiscovery() {
         super(null, null);
      }

      @Override
      public Set<String> listTenants() {
         ++this.calls;
         if (this.failing) {
            throw new IllegalStateException("central database is down");
         } else {
            return this.tenants;
         }
      }
   }

   private static final class FakeRegistry extends TenantDataSourceRegistryImpl {
      private final DataSource dataSource = mock(DataSource.class);
      private String requestedTenant;

      private FakeRegistry() {
         super(null);
      }

      @Override
      public DataSource getDataSourceForTenant(String tenantId) {
         this.requestedTenant = tenantId;
         return this.dataSource;
      }
   }

   private static final class MutableClock extends Clock {
      private Instant now = Instant.parse("2026-10-07T13:45:12Z");

      private MutableClock() {
      }

      void advance(Duration duration) {
         this.now = this.now.plus(duration);
      }

      public ZoneId getZone() {
         return ZoneId.of("UTC");
      }

      public Clock withZone(ZoneId zone) {
         return this;
      }

      public Instant instant() {
         return this.now;
      }
   }
}
