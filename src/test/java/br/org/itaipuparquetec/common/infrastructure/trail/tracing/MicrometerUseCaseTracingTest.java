package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class MicrometerUseCaseTracingTest {
   private final SdkTracerProvider provider = SdkTracerProvider.builder().build();
   private final Tracer tracer;
   private final MicrometerUseCaseTracing tracing;

   MicrometerUseCaseTracingTest() {
      this.tracer = new OtelTracer(this.provider.get("test"), new OtelCurrentTraceContext(), event -> {
      });
      this.tracing = new MicrometerUseCaseTracing(this.tracer);
   }

   @Test
   void shouldStartANewW3cTraceWhenThereIsNoSpanInEffect() {
      TraceSpan span = this.tracing.open("RegisterOrderUseCase");

      try {
         Assertions.assertThat(span.traceId()).matches("[0-9a-f]{32}");
         Assertions.assertThat(span.spanId()).matches("[0-9a-f]{16}");
         Assertions.assertThat(span.parentSpanId()).isNull();
      } catch (Throwable var5) {
         if (span != null) {
            try {
               span.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (span != null) {
         span.close();
      }

   }

   @Test
   void shouldStillGenerateIdentifiersWhenTheSamplerDropsTheSpan() {
      SdkTracerProvider neverSampling = SdkTracerProvider.builder().setSampler(Sampler.alwaysOff()).build();
      OtelTracer unsampled = new OtelTracer(neverSampling.get("test"), new OtelCurrentTraceContext(), event -> {
      });
      TraceSpan span = (new MicrometerUseCaseTracing(unsampled)).open("RegisterOrderUseCase");

      try {
         ((AbstractStringAssert)Assertions.assertThat(span.traceId()).matches("[0-9a-f]{32}")).isNotEqualTo("0".repeat(32));
         ((AbstractStringAssert)Assertions.assertThat(span.spanId()).matches("[0-9a-f]{16}")).isNotEqualTo("0".repeat(16));
      } catch (Throwable var7) {
         if (span != null) {
            try {
               span.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (span != null) {
         span.close();
      }

   }

   @Test
   void shouldOpenAChildSpanOfTheSpanInEffect() {
      TraceSpan outer = this.tracing.open("OuterUseCase");

      try {
         TraceSpan inner = this.tracing.open("InnerUseCase");

         try {
            Assertions.assertThat(inner.traceId()).isEqualTo(outer.traceId());
            Assertions.assertThat(inner.parentSpanId()).isEqualTo(outer.spanId());
            Assertions.assertThat(inner.spanId()).isNotEqualTo(outer.spanId());
         } catch (Throwable var7) {
            if (inner != null) {
               try {
                  inner.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (inner != null) {
            inner.close();
         }
      } catch (Throwable var8) {
         if (outer != null) {
            try {
               outer.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }
         }

         throw var8;
      }

      if (outer != null) {
         outer.close();
      }

   }

   @Test
   void shouldMakeTheSpanCurrentWhileOpenAndRestoreThePreviousOneWhenClosed() {
      TraceSpan outer = this.tracing.open("OuterUseCase");

      try {
         TraceSpan inner = this.tracing.open("InnerUseCase");

         try {
            Assertions.assertThat(this.tracer.currentSpan().context().spanId()).isEqualTo(inner.spanId());
         } catch (Throwable var7) {
            if (inner != null) {
               try {
                  inner.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (inner != null) {
            inner.close();
         }

         Assertions.assertThat(this.tracer.currentSpan().context().spanId()).isEqualTo(outer.spanId());
      } catch (Throwable var8) {
         if (outer != null) {
            try {
               outer.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }
         }

         throw var8;
      }

      if (outer != null) {
         outer.close();
      }

      Assertions.assertThat(this.tracer.currentSpan()).isNull();
   }
}
