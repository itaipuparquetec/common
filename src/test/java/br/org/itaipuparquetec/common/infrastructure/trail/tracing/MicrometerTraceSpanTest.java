package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.mockito.Mockito.*;

class MicrometerTraceSpanTest {
   private final Span span = mock(Span.class);
   private final TraceContext context = mock(TraceContext.class);
   private final Tracer.SpanInScope scope = mock(Tracer.SpanInScope.class);
   private final MicrometerTraceSpan traceSpan;

   MicrometerTraceSpanTest() {
      this.traceSpan = new MicrometerTraceSpan(this.span, this.scope);
      when(this.span.context()).thenReturn(this.context);
   }

   @ParameterizedTest
   @NullAndEmptySource
   @ValueSource(
      strings = {"   "}
   )
   void shouldReportNoParentWhenTheSpanHasNoParentId(String parentId) {
      when(this.context.parentId()).thenReturn(parentId);
      Assertions.assertThat(this.traceSpan.parentSpanId()).isNull();
   }

   @Test
   void shouldReportTheIdentifiersOfTheSpan() {
      when(this.context.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");
      when(this.context.spanId()).thenReturn("00f067aa0ba902b7");
      when(this.context.parentId()).thenReturn("a3ce929d0e0e4736");
      Assertions.assertThat(this.traceSpan.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
      Assertions.assertThat(this.traceSpan.spanId()).isEqualTo("00f067aa0ba902b7");
      Assertions.assertThat(this.traceSpan.parentSpanId()).isEqualTo("a3ce929d0e0e4736");
   }

   @Test
   void shouldLeaveTheScopeAndEndTheSpanWhenClosed() {
      this.traceSpan.close();
      verify(this.scope).close();
      verify(this.span).end();
   }
}
