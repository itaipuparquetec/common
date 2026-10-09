package br.org.itaipuparquetec.common.infrastructure.trail.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcTaskDecoratorTest {
   private final MdcTaskDecorator decorator = new MdcTaskDecorator();

   MdcTaskDecoratorTest() {
   }

   @AfterEach
   void cleanMdc() {
      MDC.clear();
   }

   @Test
   void shouldRunTheTaskWithTheMdcOfTheDecoratingThread() {
      MDC.put("traceId", "trace-1");
      List<String> seen = new ArrayList();
      Runnable decorated = this.decorator.decorate(() -> seen.add(MDC.get("traceId")));
      MDC.clear();
      decorated.run();
      Assertions.assertThat(seen).containsExactly(new String[]{"trace-1"});
   }

   @Test
   void shouldRestoreThePreviousMdcOfTheWorkerThreadAfterTheTask() {
      MDC.put("traceId", "parent");
      Runnable decorated = this.decorator.decorate(() -> {
      });
      MDC.clear();
      MDC.put("traceId", "worker");
      decorated.run();
      Assertions.assertThat(MDC.get("traceId")).isEqualTo("worker");
   }

   @Test
   void shouldClearTheMdcDuringTheTaskWhenTheDecoratingThreadHadNone() {
      List<String> seen = new ArrayList();
      Runnable decorated = this.decorator.decorate(() -> seen.add(MDC.get("traceId")));
      MDC.put("traceId", "worker");
      decorated.run();
      Assertions.assertThat(seen).containsExactly(new String[]{(String)null});
      Assertions.assertThat(MDC.get("traceId")).isEqualTo("worker");
   }

   @Test
   void shouldLeaveAnEmptyMdcAfterTheTaskWhenTheWorkerThreadHadNone() {
      MDC.put("traceId", "parent");
      Runnable decorated = this.decorator.decorate(() -> {
      });
      MDC.clear();
      decorated.run();
      Assertions.assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
   }

   @Test
   void shouldRestoreThePreviousMdcEvenWhenTheTaskFails() {
      MDC.put("traceId", "parent");
      Runnable decorated = this.decorator.decorate(() -> {
         throw new IllegalStateException("boom");
      });
      MDC.clear();
      MDC.put("traceId", "worker");
      Objects.requireNonNull(decorated);
      Assertions.assertThatThrownBy(decorated::run).isInstanceOf(IllegalStateException.class);
      Assertions.assertThat(MDC.get("traceId")).isEqualTo("worker");
   }
}
