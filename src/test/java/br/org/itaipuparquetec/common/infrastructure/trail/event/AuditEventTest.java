package br.org.itaipuparquetec.common.infrastructure.trail.event;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditEventFixture;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AuditEventTest {
   AuditEventTest() {
   }

   @Test
   void shouldOmitNullFieldsWhenSerialized() throws Exception {
      AuditEvent event = AuditEventFixture.failureOf("Register");
      String json = ((JsonMapper)((JsonMapper.Builder)JsonMapper.builder().findAndAddModules()).build()).writeValueAsString(event);
      ((AbstractStringAssert)Assertions.assertThat(json).contains(new CharSequence[]{"\"useCase\":\"Register\"", "\"result\":\"BUSINESS_ERROR\""})).doesNotContain(new CharSequence[]{"\"actor\"", "\"input\"", "\"output\"", "\"parentSpanId\"", "\"sid\""});
   }
}
