package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.application.usecases.NullaryUseCase;
import br.org.itaipuparquetec.common.application.usecases.UnitUseCase;
import br.org.itaipuparquetec.common.application.usecases.UseCase;
import br.org.itaipuparquetec.common.domain.exceptions.DomainException;
import br.org.itaipuparquetec.common.domain.exceptions.TechnicalException;
import br.org.itaipuparquetec.common.infrastructure.trail.envelope.UuidV7Generator;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditErrorCategory;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditResult;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditActorResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditActorType;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.SourceIdentity;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.StandaloneUseCaseTracing;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AuditTrailAspectTest {
   private final RecordingAuditEventSink sink = new RecordingAuditEventSink();

   AuditTrailAspectTest() {
   }

   @AfterEach
   void cleanState() {
      MDC.clear();
      TransactionSynchronizationManager.clear();
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
   }

   @Test
   void shouldPublishASuccessEventWithIdentityTenantAndSource() {
      RegisterOrderUseCaseImpl useCase = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.ofUser("user-1"));
      String output = useCase.execute("order-7");
      AuditEvent event = this.sink.onlyEvent();
      Assertions.assertThat(output).isEqualTo("done:order-7");
      Assertions.assertThat(event.result()).isEqualTo(AuditResult.SUCCESS);
      Assertions.assertThat(event.useCase()).isEqualTo("RegisterOrderUseCase");
      Assertions.assertThat(event.useCaseVersion()).isEqualTo("1");
      Assertions.assertThat(event.actor()).isEqualTo("user-1");
      Assertions.assertThat(event.actorType()).isEqualTo(AuditActorType.USER);
      Assertions.assertThat(event.sid()).isEqualTo("sid-1");
      Assertions.assertThat(event.jti()).isEqualTo("jti-1");
      Assertions.assertThat(event.actorTenant()).isEqualTo("acme");
      Assertions.assertThat(event.tenant()).isEqualTo("acme_tenant");
      Assertions.assertThat(event.sourceService()).isEqualTo("mirror");
      Assertions.assertThat(event.sourceVersion()).isEqualTo("0.8.0");
      Assertions.assertThat(event.input()).isEqualTo("order-7");
      Assertions.assertThat(event.error()).isNull();
      Assertions.assertThat(event.eventId().version()).isEqualTo(7);
      Assertions.assertThat(event.occurredAt()).isNotNull();
      Assertions.assertThat(event.durationMillis()).isNotNegative();
   }

   @Test
   void shouldStartAW3cTraceWhenThereIsNoTraceInTheContext() {
      RegisterOrderUseCaseImpl useCase = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.withoutToken());
      useCase.execute("order-7");
      AuditEvent event = this.sink.onlyEvent();
      Assertions.assertThat(event.traceId()).matches("[0-9a-f]{32}");
      Assertions.assertThat(event.spanId()).matches("[0-9a-f]{16}");
      Assertions.assertThat(event.parentSpanId()).isNull();
      Assertions.assertThat(event.actor()).isNull();
      Assertions.assertThat(event.actorType()).isEqualTo(AuditActorType.SYSTEM);
   }

   @Test
   void shouldContinueTheTraceAlreadyInTheContextAsAChildSpan() {
      MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
      MDC.put("spanId", "00f067aa0ba902b7");
      RegisterOrderUseCaseImpl useCase = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.ofService("client-1"));
      useCase.execute("order-7");
      AuditEvent event = this.sink.onlyEvent();
      Assertions.assertThat(event.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
      Assertions.assertThat(event.parentSpanId()).isEqualTo("00f067aa0ba902b7");
      Assertions.assertThat(event.spanId()).isNotEqualTo("00f067aa0ba902b7");
      Assertions.assertThat(event.actorType()).isEqualTo(AuditActorType.SERVICE);
   }

   @Test
   void shouldExposeTheContextInTheMdcDuringExecutionAndRestoreItAfterwards() {
      ContextInspectingUseCase inspecting = new ContextInspectingUseCase();
      ContextInspectingUseCase useCase = (ContextInspectingUseCase)this.proxyOf(inspecting, FakeTokenClaimsReader.ofUser("user-1"));
      useCase.execute("order-7");
      Assertions.assertThat(inspecting.actorSeen).isEqualTo("user-1");
      Assertions.assertThat(inspecting.sidSeen).isEqualTo("sid-1");
      Assertions.assertThat(inspecting.traceSeen).isEqualTo(this.sink.onlyEvent().traceId());
      Assertions.assertThat(MDC.get("actor")).isNull();
      Assertions.assertThat(MDC.get("traceId")).isNull();
   }

   @Test
   void shouldKeepTheContextOfTheOuterUseCaseWhenANestedUseCaseEnds() {
      RegisterOrderUseCaseImpl inner = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.ofUser("user-1"));
      OuterUseCaseImpl outerTarget = new OuterUseCaseImpl(inner);
      OuterUseCaseImpl outer = (OuterUseCaseImpl)this.proxyOf(outerTarget, FakeTokenClaimsReader.ofUser("user-1"));
      outer.execute("order-7");
      Assertions.assertThat(outerTarget.actorAfterInner).isEqualTo("user-1");
      Assertions.assertThat(outerTarget.traceAfterInner).isNotNull();
      Assertions.assertThat(outerTarget.spanAfterInner).isEqualTo(outerTarget.spanBeforeInner);
      Assertions.assertThat(MDC.get("actor")).isNull();
   }

   @Test
   void shouldRecordTheNestedUseCaseAsAChildOfTheOuterOneInTheSameTrace() {
      RegisterOrderUseCaseImpl inner = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.ofUser("user-1"));
      OuterUseCaseImpl outer = (OuterUseCaseImpl)this.proxyOf(new OuterUseCaseImpl(inner), FakeTokenClaimsReader.ofUser("user-1"));
      outer.execute("order-7");
      AuditEvent innerEvent = (AuditEvent)this.sink.events().get(0);
      AuditEvent outerEvent = (AuditEvent)this.sink.events().get(1);
      Assertions.assertThat(innerEvent.traceId()).isEqualTo(outerEvent.traceId());
      Assertions.assertThat(innerEvent.parentSpanId()).isEqualTo(outerEvent.spanId());
      Assertions.assertThat(outerEvent.parentSpanId()).isNull();
   }

   @Test
   void shouldRecordTheVersionDeclaredByTheAuditVersionAnnotation() {
      VersionedUseCaseImpl useCase = (VersionedUseCaseImpl)this.proxyOf(new VersionedUseCaseImpl(), FakeTokenClaimsReader.withoutToken());
      useCase.execute("order-7");
      Assertions.assertThat(this.sink.onlyEvent().useCaseVersion()).isEqualTo("3");
   }

   @Test
   void shouldKeepTheClassNameWhenItHasNoImplSuffix() {
      CancelOrder useCase = (CancelOrder)this.proxyOf(new CancelOrder(), FakeTokenClaimsReader.withoutToken());
      useCase.execute("order-7");
      Assertions.assertThat(this.sink.onlyEvent().useCase()).isEqualTo("CancelOrder");
   }

   @Test
   void shouldPublishABusinessErrorWithoutTheMessageAndRethrowWhenADomainExceptionIsThrown() {
      FailingUseCase useCase = (FailingUseCase)this.proxyOf(new FailingUseCase(new OrderRejectedException("rejected john@doe.com")), FakeTokenClaimsReader.ofUser("user-1"));
      Assertions.assertThatThrownBy(() -> useCase.execute("order-7")).isInstanceOf(OrderRejectedException.class);
      AuditEvent event = this.sink.onlyEvent();
      Assertions.assertThat(event.result()).isEqualTo(AuditResult.BUSINESS_ERROR);
      Assertions.assertThat(event.error().type()).isEqualTo("OrderRejectedException");
      Assertions.assertThat(event.error().category()).isEqualTo(AuditErrorCategory.BUSINESS);
      Assertions.assertThat(event.error().toString()).doesNotContain(new CharSequence[]{"john@doe.com"});
      Assertions.assertThat(event.input()).isEqualTo("order-7");
   }

   @Test
   void shouldPublishATechnicalErrorWhenATechnicalExceptionIsThrown() {
      FailingUseCase useCase = (FailingUseCase)this.proxyOf(new FailingUseCase(new TechnicalException("db password=secret")), FakeTokenClaimsReader.withoutToken());
      Assertions.assertThatThrownBy(() -> useCase.execute("order-7")).isInstanceOf(TechnicalException.class);
      AuditEvent event = this.sink.onlyEvent();
      Assertions.assertThat(event.result()).isEqualTo(AuditResult.TECHNICAL_ERROR);
      Assertions.assertThat(event.error().type()).isEqualTo("TechnicalException");
      Assertions.assertThat(event.error().toString()).doesNotContain(new CharSequence[]{"secret"});
   }

   @Test
   void shouldPublishATechnicalErrorWhenAnUnexpectedExceptionIsThrown() {
      FailingUseCase useCase = (FailingUseCase)this.proxyOf(new FailingUseCase(new IllegalStateException("boom")), FakeTokenClaimsReader.withoutToken());
      Assertions.assertThatThrownBy(() -> useCase.execute("order-7")).isInstanceOf(IllegalStateException.class);
      Assertions.assertThat(this.sink.onlyEvent().result()).isEqualTo(AuditResult.TECHNICAL_ERROR);
   }

   @Test
   void shouldFailTheUseCaseWhenTheEventOfASuccessCannotBeDelivered() {
      this.sink.failWith(new IllegalStateException("outbox down"));
      RegisterOrderUseCaseImpl useCase = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.withoutToken());
      ((AbstractThrowableAssert)Assertions.assertThatThrownBy(() -> useCase.execute("order-7")).isInstanceOf(IllegalStateException.class)).hasMessage("outbox down");
      Assertions.assertThat(this.sink.events()).hasSize(1);
   }

   @Test
   void shouldNeverMaskTheOriginalFailureWhenTheEventOfAFailureCannotBeDelivered() {
      this.sink.failWith(new IllegalStateException("outbox down"));
      FailingUseCase useCase = (FailingUseCase)this.proxyOf(new FailingUseCase(new OrderRejectedException("rejected")), FakeTokenClaimsReader.withoutToken());
      ((AbstractThrowableAssert)Assertions.assertThatThrownBy(() -> useCase.execute("order-7")).isInstanceOf(OrderRejectedException.class)).hasSuppressedException(new IllegalStateException("outbox down"));
   }

   @Test
   void shouldAuditAUnitUseCaseUsingItsInput() {
      ArchiveOrderUseCaseImpl useCase = (ArchiveOrderUseCaseImpl)this.proxyOf(new ArchiveOrderUseCaseImpl(), FakeTokenClaimsReader.withoutToken());
      useCase.execute("order-7");
      AuditEvent event = this.sink.onlyEvent();
      Assertions.assertThat(event.useCase()).isEqualTo("ArchiveOrderUseCase");
      Assertions.assertThat(event.input()).isEqualTo("order-7");
   }

   @Test
   void shouldAuditANullaryUseCaseWithoutInput() {
      CountOrdersUseCaseImpl useCase = (CountOrdersUseCaseImpl)this.proxyOf(new CountOrdersUseCaseImpl(), FakeTokenClaimsReader.withoutToken());
      Integer output = useCase.execute();
      Assertions.assertThat(output).isEqualTo(42);
      Assertions.assertThat(this.sink.onlyEvent().input()).isNull();
   }

   @Test
   void shouldSkipTheAuditOfAUseCaseRunningInAReadOnlyTransaction() {
      beginReadOnlyTransaction();
      RegisterOrderUseCaseImpl useCase = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.withoutToken());
      String output = useCase.execute("order-7");
      Assertions.assertThat(output).isEqualTo("done:order-7");
      Assertions.assertThat(this.sink.events()).isEmpty();
   }

   @Test
   void shouldAuditAMarkedSensitiveReadInAReadOnlyTransaction() {
      beginReadOnlyTransaction();
      ReadComplaintUseCaseImpl useCase = (ReadComplaintUseCaseImpl)this.proxyOf(new ReadComplaintUseCaseImpl(), FakeTokenClaimsReader.withoutToken());
      useCase.execute("complaint-1");
      Assertions.assertThat(this.sink.onlyEvent().useCase()).isEqualTo("ReadComplaintUseCase");
   }

   @Test
   void shouldAuditAnyUseCaseInAReadOnlyTransactionWhenReadOnlyIsIncluded() {
      beginReadOnlyTransaction();
      RegisterOrderUseCaseImpl useCase = (RegisterOrderUseCaseImpl)this.proxyOf(new RegisterOrderUseCaseImpl(), FakeTokenClaimsReader.withoutToken(), true);
      useCase.execute("order-7");
      Assertions.assertThat(this.sink.events()).hasSize(1);
   }

   private static void beginReadOnlyTransaction() {
      TransactionSynchronizationManager.initSynchronization();
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
   }

   private <T> T proxyOf(T target, FakeTokenClaimsReader claims) {
      return (T)this.proxyOf(target, claims, false);
   }

   private <T> T proxyOf(T target, FakeTokenClaimsReader claims, boolean includeReadOnly) {
      Clock clock = Clock.fixed(Instant.parse("2026-10-07T13:45:12.345Z"), ZoneOffset.UTC);
      AuditEventAssembler assembler = new AuditEventAssembler(new UuidV7Generator(clock, new Random(7L)), new SourceIdentity("mirror", "0.8.0"), new AuditTenantResolver(() -> "acme_tenant"));
      AuditTrailAspect aspect = new AuditTrailAspect(this.sink, new AuditActorResolver(claims), new StandaloneUseCaseTracing(new Random(11L)), assembler, new AuditPolicy(includeReadOnly));
      AspectJProxyFactory factory = new AspectJProxyFactory(target);
      factory.setProxyTargetClass(true);
      factory.addAspect(aspect);
      return (T)factory.getProxy();
   }

   public static class RegisterOrderUseCaseImpl implements UseCase<String, String> {
      public String execute(String input) {
         return "done:" + input;
      }
   }

   public static class ContextInspectingUseCase implements UseCase<String, String> {
      private String actorSeen;
      private String sidSeen;
      private String traceSeen;

      public String execute(String input) {
         this.actorSeen = MDC.get("actor");
         this.sidSeen = MDC.get("sid");
         this.traceSeen = MDC.get("traceId");
         return input;
      }
   }

   public static class OuterUseCaseImpl implements UseCase<String, String> {
      private final UseCase<String, String> inner;
      private String spanBeforeInner;
      private String actorAfterInner;
      private String traceAfterInner;
      private String spanAfterInner;

      public OuterUseCaseImpl(UseCase<String, String> inner) {
         this.inner = inner;
      }

      public String execute(String input) {
         this.spanBeforeInner = MDC.get("spanId");
         String output = (String)this.inner.execute(input);
         this.actorAfterInner = MDC.get("actor");
         this.traceAfterInner = MDC.get("traceId");
         this.spanAfterInner = MDC.get("spanId");
         return output;
      }
   }

   @AuditVersion("3")
   public static class VersionedUseCaseImpl implements UseCase<String, String> {
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

   public static class CancelOrder implements UseCase<String, String> {
      public String execute(String input) {
         return input;
      }
   }

   public static class FailingUseCase implements UseCase<String, String> {
      private final RuntimeException failure;

      public FailingUseCase(RuntimeException failure) {
         this.failure = failure;
      }

      public String execute(String input) {
         throw this.failure;
      }
   }

   public static class ArchiveOrderUseCaseImpl implements UnitUseCase<String> {
      public void execute(String input) {
         // no-op: this unit use case performs no work; the test only asserts the audit event
      }
   }

   public static class CountOrdersUseCaseImpl implements NullaryUseCase<Integer> {
      public Integer execute() {
         return 42;
      }
   }

   public static class OrderRejectedException extends RuntimeException implements DomainException {
      public OrderRejectedException(String message) {
         super(message);
      }
   }
}
