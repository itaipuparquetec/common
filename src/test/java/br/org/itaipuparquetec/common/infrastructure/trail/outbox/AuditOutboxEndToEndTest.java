package br.org.itaipuparquetec.common.infrastructure.trail.outbox;

import br.org.itaipuparquetec.common.application.usecases.NullaryUseCase;
import br.org.itaipuparquetec.common.application.usecases.UseCase;
import br.org.itaipuparquetec.common.domain.exceptions.DomainException;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditOutboxAutoConfiguration;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditSensitiveRead;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditTrailAutoConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.util.List;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AuditOutboxEndToEndTest {
   private static final ObjectMapper JSON = new ObjectMapper();
   private final String schema = PostgresTestDatabase.newSchemaName();
   private final HikariDataSource dataSource;
   private final JdbcTemplate jdbc;
   private final SimpleMeterRegistry meters;

   AuditOutboxEndToEndTest() {
      this.dataSource = PostgresTestDatabase.dataSourceWithOutboxTable(this.schema, 1);
      this.jdbc = new JdbcTemplate(this.dataSource);
      this.meters = new SimpleMeterRegistry();
   }

   @BeforeEach
   void createBusinessTable() {
      this.jdbc.execute("CREATE TABLE orders (id varchar(40) PRIMARY KEY)");
   }

   @AfterEach
   void closePool() {
      this.dataSource.close();
   }

   private ApplicationContextRunner runner() {
      return (ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)(new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(new Class[]{HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class, AuditTrailAutoConfiguration.class, AuditOutboxAutoConfiguration.class}))).withUserConfiguration(new Class[]{EntityScanning.class})).withBean(DataSource.class, () -> this.dataSource, new BeanDefinitionCustomizer[0])).withBean(MeterRegistry.class, () -> this.meters, new BeanDefinitionCustomizer[0])).withBean(PlaceOrderUseCaseImpl.class, new Object[0])).withBean(ReadOrderUseCaseImpl.class, new Object[0])).withBean(ReadComplaintUseCaseImpl.class, new Object[0])).withBean(CountingUseCaseImpl.class, new Object[0])).withPropertyValues(new String[]{"spring.application.name=mirror", "audit.enabled=true", "audit.sink=outbox", "spring.jpa.hibernate.ddl-auto=none"});
   }

   @Test
   void shouldStoreTheEventOfASuccessInTheSameTransactionAsTheBusinessChange() {
      this.runner().run(context -> {
         inTransaction(context, false, () -> ((PlaceOrderUseCaseImpl)context.getBean(PlaceOrderUseCaseImpl.class)).execute("order-1"));
         Assertions.assertThat(this.orderIds()).containsExactly(new String[]{"order-1"});
         JsonNode event = this.onlyEvent();
         Assertions.assertThat(event.get("result").asText()).isEqualTo("SUCCESS");
         Assertions.assertThat(event.get("useCase").asText()).isEqualTo("PlaceOrderUseCase");
         Assertions.assertThat(event.get("sourceService").asText()).isEqualTo("mirror");
         Assertions.assertThat(event.get("input").asText()).isEqualTo("\"order-1\"");
         Assertions.assertThat(event.get("traceId").asText()).matches("[0-9a-f]{32}");
      });
   }

   @Test
   void shouldLoseTheEventOfASuccessTogetherWithTheBusinessChangeWhenTheTransactionRollsBack() {
      this.runner().run(context -> {
         Assertions.assertThatThrownBy(() -> inTransaction(context, false, () -> {
               ((PlaceOrderUseCaseImpl)context.getBean(PlaceOrderUseCaseImpl.class)).execute("order-1");
               throw new IllegalStateException("controller failed after the use case");
            })).isInstanceOf(IllegalStateException.class);
         Assertions.assertThat(this.orderIds()).isEmpty();
         Assertions.assertThat(this.events()).isEmpty();
      });
   }

   @Test
   void shouldStoreTheEventOfAFailureAfterTheRollbackWithoutHoldingTwoConnections() {
      this.runner().run(context -> {
         Assertions.assertThatThrownBy(() -> inTransaction(context, false, () -> ((PlaceOrderUseCaseImpl)context.getBean(PlaceOrderUseCaseImpl.class)).execute("duplicate"))).isInstanceOf(OrderRejectedException.class);
         Assertions.assertThat(this.orderIds()).isEmpty();
         JsonNode event = this.eventWrittenAfterTheTransaction();
         Assertions.assertThat(event.get("result").asText()).isEqualTo("BUSINESS_ERROR");
         Assertions.assertThat(event.get("error").get("type").asText()).isEqualTo("OrderRejectedException");
         Assertions.assertThat(event.get("error").get("category").asText()).isEqualTo("BUSINESS");
         Assertions.assertThat(event.toString()).doesNotContain(new CharSequence[]{"private detail"});
      });
   }

   @Test
   void shouldSkipAReadOnlyUseCaseThatIsNotMarkedAsSensitive() {
      this.runner().run(context -> {
         inTransaction(context, true, () -> ((ReadOrderUseCaseImpl)context.getBean(ReadOrderUseCaseImpl.class)).execute("order-1"));
         Assertions.assertThat(this.events()).isEmpty();
      });
   }

   @Test
   void shouldStoreTheEventOfAMarkedSensitiveReadAfterTheReadOnlyTransactionEnds() {
      this.runner().run(context -> {
         inTransaction(context, true, () -> ((ReadComplaintUseCaseImpl)context.getBean(ReadComplaintUseCaseImpl.class)).execute("complaint-1"));
         JsonNode event = this.eventWrittenAfterTheTransaction();
         Assertions.assertThat(event.get("useCase").asText()).isEqualTo("ReadComplaintUseCase");
         Assertions.assertThat(event.get("result").asText()).isEqualTo("SUCCESS");
      });
   }

   @Test
   void shouldStoreEveryReadWhenReadOnlyUseCasesAreIncluded() {
      ((ApplicationContextRunner)this.runner().withPropertyValues(new String[]{"audit.include-read-only=true"})).run(context -> {
         inTransaction(context, true, () -> ((ReadOrderUseCaseImpl)context.getBean(ReadOrderUseCaseImpl.class)).execute("order-1"));
         Assertions.assertThat(this.eventWrittenAfterTheTransaction().get("useCase").asText()).isEqualTo("ReadOrderUseCase");
      });
   }

   @Test
   void shouldStoreTheEventImmediatelyInATransactionOfItsOwnWhenThereIsNoTransaction() {
      this.runner().run(context -> {
         ((CountingUseCaseImpl)context.getBean(CountingUseCaseImpl.class)).execute();
         Assertions.assertThat(this.onlyEvent().get("useCase").asText()).isEqualTo("CountingUseCase");
      });
   }

   @Test
   void shouldFailTheBusinessOperationWhenTheOutboxCannotStoreTheEventOfASuccess() {
      this.runner().run(context -> {
         this.jdbc.execute("DROP TABLE audit_outbox");
         Assertions.assertThatThrownBy(() -> inTransaction(context, false, () -> ((PlaceOrderUseCaseImpl)context.getBean(PlaceOrderUseCaseImpl.class)).execute("order-1"))).isInstanceOf(RuntimeException.class);
         Assertions.assertThat(this.orderIds()).isEmpty();
      });
   }

   @Test
   void shouldFallBackToTheLogAndCountTheFailureWhenTheOutboxCannotStoreTheEventOfAFailure() {
      this.runner().run(context -> {
         this.jdbc.execute("DROP TABLE audit_outbox");
         Assertions.assertThatThrownBy(() -> inTransaction(context, false, () -> ((PlaceOrderUseCaseImpl)context.getBean(PlaceOrderUseCaseImpl.class)).execute("duplicate"))).isInstanceOf(OrderRejectedException.class);
         Awaitility.await().atMost(Duration.ofSeconds(10L)).untilAsserted(() -> Assertions.assertThat(this.meters.counter("audit_outbox_write_failures", new String[]{"service", "mirror"}).count()).isEqualTo((double)1.0F));
      });
   }

   private static void inTransaction(ApplicationContext context, boolean readOnly, Runnable work) {
      TransactionTemplate template = new TransactionTemplate((PlatformTransactionManager)context.getBean(PlatformTransactionManager.class));
      template.setReadOnly(readOnly);
      template.executeWithoutResult(status -> work.run());
   }

   private List<String> orderIds() {
      return this.jdbc.queryForList("SELECT id FROM orders", String.class);
   }

   private List<String> events() {
      return this.jdbc.queryForList("SELECT payload FROM audit_outbox ORDER BY event_id", String.class);
   }

   private JsonNode onlyEvent() throws Exception {
      List<String> events = this.events();
      Assertions.assertThat(events).hasSize(1);
      return JSON.readTree((String)events.get(0));
   }

   private JsonNode eventWrittenAfterTheTransaction() throws Exception {
      Awaitility.await().atMost(Duration.ofSeconds(10L)).until(() -> !this.events().isEmpty());
      return this.onlyEvent();
   }

   @Configuration(
      proxyBeanMethods = false
   )
   @EntityScan(
      basePackageClasses = {AuditOutboxEndToEndTest.class}
   )
   static class EntityScanning {
      EntityScanning() {
      }
   }

   @Entity
   @Table(
      name = "orders"
   )
   public static class OrderJpa {
      @Id
      private String id;

      protected OrderJpa() {
      }

      OrderJpa(String id) {
         this.id = id;
      }
   }

   public static class PlaceOrderUseCaseImpl implements UseCase<String, String> {
      private final EntityManager entityManager;

      public PlaceOrderUseCaseImpl(EntityManagerFactory entityManagerFactory) {
         this.entityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
      }

      public String execute(String input) {
         if ("duplicate".equals(input)) {
            this.entityManager.persist(new OrderJpa(input));
            this.entityManager.flush();
            throw new OrderRejectedException("order already exists: private detail");
         } else {
            this.entityManager.persist(new OrderJpa(input));
            this.entityManager.flush();
            return input;
         }
      }
   }

   public static class ReadOrderUseCaseImpl implements UseCase<String, String> {
      public String execute(String input) {
         return input;
      }
   }

   @AuditSensitiveRead
   public static class ReadComplaintUseCaseImpl implements UseCase<String, String> {
      public String execute(String input) {
         return input;
      }
   }

   public static class CountingUseCaseImpl implements NullaryUseCase<Integer> {
      public Integer execute() {
         return 1;
      }
   }

   public static class OrderRejectedException extends RuntimeException implements DomainException {
      public OrderRejectedException(String message) {
         super(message);
      }
   }
}
