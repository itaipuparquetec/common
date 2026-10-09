package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

import java.util.Random;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

class StandaloneUseCaseTracingTest {
   private final StandaloneUseCaseTracing tracing = new StandaloneUseCaseTracing(new Random(3L));

   StandaloneUseCaseTracingTest() {
   }

   @AfterEach
   void cleanMdc() {
      MDC.clear();
   }

   @Test
   void shouldStartANewRootTraceWhenTheContextHasNone() {
      TraceSpan span = this.tracing.open("RegisterOrderUseCase");
      Assertions.assertThat(span.traceId()).matches("[0-9a-f]{32}");
      Assertions.assertThat(span.spanId()).matches("[0-9a-f]{16}");
      Assertions.assertThat(span.parentSpanId()).isNull();
   }

   @ParameterizedTest
   @ValueSource(
      strings = {"d4f5c9b2-6a3e-4c7a-9a55-0f1c2d3e4f50", "TOO-SHORT", "4BF92F3577B34DA6A3CE929D0E0E4736"}
   )
   void shouldStartANewTraceWhenTheTraceIdInTheContextIsNotW3c(String legacyTraceId) {
      MDC.put("traceId", legacyTraceId);
      MDC.put("spanId", "00f067aa0ba902b7");
      TraceSpan span = this.tracing.open("RegisterOrderUseCase");
      ((AbstractStringAssert)Assertions.assertThat(span.traceId()).matches("[0-9a-f]{32}")).isNotEqualTo(legacyTraceId);
      Assertions.assertThat(span.parentSpanId()).isNull();
   }

   @Test
   void shouldContinueTheW3cTraceInTheContextMakingTheCurrentSpanTheParent() {
      MDC.put("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
      MDC.put("spanId", "00f067aa0ba902b7");
      TraceSpan span = this.tracing.open("RegisterOrderUseCase");
      Assertions.assertThat(span.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
      Assertions.assertThat(span.parentSpanId()).isEqualTo("00f067aa0ba902b7");
      ((AbstractStringAssert)Assertions.assertThat(span.spanId()).matches("[0-9a-f]{16}")).isNotEqualTo("00f067aa0ba902b7");
   }

   @Test
   void shouldCloseWithoutAnyEffect() {
      TraceSpan span = this.tracing.open("RegisterOrderUseCase");
      span.close();
      Assertions.assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
   }
}
