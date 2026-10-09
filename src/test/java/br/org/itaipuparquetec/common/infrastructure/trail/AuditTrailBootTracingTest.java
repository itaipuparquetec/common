package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.application.usecases.UseCase;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.MicrometerUseCaseTracing;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.UseCaseTracing;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuditTrailBootTracingTest {
   private final RecordingAuditEventSink sink = new RecordingAuditEventSink();
   private final ApplicationContextRunner runner = (ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)((ApplicationContextRunner)(new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(new Class[]{ObservationAutoConfiguration.class, MicrometerTracingAutoConfiguration.class, OpenTelemetryAutoConfiguration.class, OpenTelemetryTracingAutoConfiguration.class, AuditTrailAutoConfiguration.class}))).withBean(AuditEventSink.class, () -> this.sink, new BeanDefinitionCustomizer[0])).withBean(PlaceOrderUseCaseImpl.class, new Object[0])).withPropertyValues(new String[]{"audit.enabled=true", "audit.sink=outbox", "spring.application.name=mirror", "management.tracing.sampling.probability=1.0"});

   AuditTrailBootTracingTest() {
   }

   @AfterEach
   void cleanMdc() {
      MDC.clear();
   }

   @Test
   void shouldUseTheMicrometerTracerConfiguredByTheBootStack() {
      this.runner.run(context -> Assertions.assertThat((UseCaseTracing)context.getBean(UseCaseTracing.class)).isInstanceOf(MicrometerUseCaseTracing.class));
   }

   @Test
   void shouldRecordTheUseCaseAsAChildOfTheSpanInEffectInTheSameTrace() {
      this.runner.run(context -> {
         Tracer tracer = (Tracer)context.getBean(Tracer.class);
         Span parent = tracer.nextSpan().name("http server request").start();
         Tracer.SpanInScope ignored = tracer.withSpan(parent);

         try {
            ((PlaceOrderUseCaseImpl)context.getBean(PlaceOrderUseCaseImpl.class)).execute("order-1");
         } catch (Throwable var8) {
            if (ignored != null) {
               try {
                  ignored.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (ignored != null) {
            ignored.close();
         }

         AuditEvent event = this.sink.onlyEvent();
         Assertions.assertThat(event.traceId()).isEqualTo(parent.context().traceId());
         Assertions.assertThat(event.parentSpanId()).isEqualTo(parent.context().spanId());
         ((AbstractStringAssert)Assertions.assertThat(event.spanId()).matches("[0-9a-f]{16}")).isNotEqualTo(parent.context().spanId());
         parent.end();
      });
   }

   @Test
   void shouldStartATraceWhenNoSpanIsInEffectAndPutTheIdentifiersInTheMdcWhileRunning() {
      this.runner.run(context -> {
         PlaceOrderUseCaseImpl useCase = (PlaceOrderUseCaseImpl)context.getBean(PlaceOrderUseCaseImpl.class);
         useCase.execute("order-1");
         AuditEvent event = this.sink.onlyEvent();
         Assertions.assertThat(event.traceId()).matches("[0-9a-f]{32}");
         Assertions.assertThat(event.parentSpanId()).isNull();
         Assertions.assertThat(useCase.traceInMdc()).isEqualTo(event.traceId());
         Assertions.assertThat(useCase.spanInMdc()).isEqualTo(event.spanId());
         Assertions.assertThat(MDC.get("traceId")).isNull();
      });
   }

   public static class PlaceOrderUseCaseImpl implements UseCase<String, String> {
      private String traceInMdc;
      private String spanInMdc;

      public String traceInMdc() {
         return this.traceInMdc;
      }

      public String spanInMdc() {
         return this.spanInMdc;
      }

      public String execute(String input) {
         this.traceInMdc = MDC.get("traceId");
         this.spanInMdc = MDC.get("spanId");
         return input;
      }
   }
}
