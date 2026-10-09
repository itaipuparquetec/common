package br.org.itaipuparquetec.common.infrastructure.trail.context;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class AuditContextTest {
   AuditContextTest() {
   }

   @AfterEach
   void cleanMdc() {
      MDC.clear();
   }

   @Test
   void shouldPutEveryPresentIdentifierInTheMdc() {
      AuditContextScope ignored = AuditContext.open("user-1", "sid-1", "jti-1", "trace-1", "span-1");

      try {
         Assertions.assertThat(MDC.get("actor")).isEqualTo("user-1");
         Assertions.assertThat(MDC.get("sid")).isEqualTo("sid-1");
         Assertions.assertThat(MDC.get("jti")).isEqualTo("jti-1");
         Assertions.assertThat(MDC.get("traceId")).isEqualTo("trace-1");
         Assertions.assertThat(MDC.get("spanId")).isEqualTo("span-1");
      } catch (Throwable var5) {
         if (ignored != null) {
            try {
               ignored.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (ignored != null) {
         ignored.close();
      }

   }

   @Test
   void shouldSkipNullIdentifiers() {
      AuditContextScope ignored = AuditContext.open((String)null, (String)null, (String)null, (String)null, (String)null);

      try {
         Assertions.assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
      } catch (Throwable var5) {
         if (ignored != null) {
            try {
               ignored.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (ignored != null) {
         ignored.close();
      }

   }

   @Test
   void shouldRemoveTheIdentifiersWhenTheScopeClosesAndKeepUnrelatedEntries() {
      MDC.put("other", "kept");
      AuditContextScope ignored = AuditContext.open("user-1", "sid-1", "jti-1", "trace-1", "span-1");

      try {
         MDC.put("inside", "removed-by-nobody");
      } catch (Throwable var5) {
         if (ignored != null) {
            try {
               ignored.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (ignored != null) {
         ignored.close();
      }

      Assertions.assertThat(MDC.getCopyOfContextMap()).containsOnlyKeys(new String[]{"other", "inside"});
   }

   @Test
   void shouldRestoreTheContextOfTheOuterExecutionWhenANestedOneEnds() {
      AuditContextScope outer = AuditContext.open("user-1", "sid-1", "jti-1", "trace-1", "outer-span");

      try {
         AuditContextScope inner = AuditContext.open("user-1", "sid-1", "jti-1", "trace-1", "inner-span");

         try {
            Assertions.assertThat(MDC.get("spanId")).isEqualTo("inner-span");
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

         Assertions.assertThat(MDC.get("actor")).isEqualTo("user-1");
         Assertions.assertThat(MDC.get("traceId")).isEqualTo("trace-1");
         Assertions.assertThat(MDC.get("spanId")).isEqualTo("outer-span");
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
}
