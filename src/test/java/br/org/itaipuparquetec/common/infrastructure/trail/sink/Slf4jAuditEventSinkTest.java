package br.org.itaipuparquetec.common.infrastructure.trail.sink;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditEventFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class Slf4jAuditEventSinkTest {
   private final Logger auditLogger = (Logger)LoggerFactory.getLogger("AUDIT");
   private final ListAppender<ILoggingEvent> appender = new ListAppender();

   Slf4jAuditEventSinkTest() {
   }

   @BeforeEach
   void attachAppender() {
      this.appender.start();
      this.auditLogger.addAppender(this.appender);
   }

   @AfterEach
   void detachAppender() {
      this.auditLogger.detachAppender(this.appender);
   }

   @Test
   void shouldLogTheEventAsJsonUnderTheAuditLogger() {
      Slf4jAuditEventSink sink = new Slf4jAuditEventSink(((JsonMapper.Builder)JsonMapper.builder().findAndAddModules()).build());
      sink.publish(eventOf("Register"));
      Assertions.assertThat(this.appender.list).hasSize(1);
      Assertions.assertThat(((ILoggingEvent)this.appender.list.get(0)).getLevel()).isEqualTo(Level.INFO);
      Assertions.assertThat(((ILoggingEvent)this.appender.list.get(0)).getFormattedMessage()).contains(new CharSequence[]{"\"useCase\":\"Register\""});
   }

   @Test
   void shouldLogAnErrorWithoutTheEventBodyWhenSerializationFails() {
      Slf4jAuditEventSink sink = new Slf4jAuditEventSink(new FailingObjectMapper());
      sink.publish(eventOf("Register"));
      Assertions.assertThat(this.appender.list).hasSize(1);
      Assertions.assertThat(((ILoggingEvent)this.appender.list.get(0)).getLevel()).isEqualTo(Level.ERROR);
      Assertions.assertThat(((ILoggingEvent)this.appender.list.get(0)).getFormattedMessage()).contains(new CharSequence[]{"Register"});
   }

   private static AuditEvent eventOf(String useCase) {
      return AuditEventFixture.successOf(useCase);
   }

   private static final class FailingObjectMapper extends ObjectMapper {
      private FailingObjectMapper() {
      }

      @Override
      public String writeValueAsString(Object value) throws JsonProcessingException {
         throw new JsonProcessingException("cannot serialize") {
         };
      }
   }
}
