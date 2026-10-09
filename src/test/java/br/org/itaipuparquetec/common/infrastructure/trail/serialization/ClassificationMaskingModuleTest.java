package br.org.itaipuparquetec.common.infrastructure.trail.serialization;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditProperties;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditPropertiesFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Confidential;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Internal;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Public;
import br.org.itaipuparquetec.common.infrastructure.trail.annotation.Sensitive;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class ClassificationMaskingModuleTest {
   ClassificationMaskingModuleTest() {
   }

   @ParameterizedTest
   @CsvSource({"true, true, true, pub, int, conf", "true, true, false, pub, int, PSEUDO", "false, true, false, REDACTED, int, PSEUDO", "true, false, false, pub, REDACTED, PSEUDO"})
   void shouldMaskEachClassifiedPropertyAccordingToTheCleartextFlags(boolean publicData, boolean internal, boolean confidential, String expectedPublic, String expectedInternal, String expectedConfidential) throws Exception {
      ObjectMapper mapper = mapperWith(publicData, internal, confidential);
      JsonNode json = mapper.readTree(mapper.writeValueAsString(sample()));
      Assertions.assertThat(json.get("sensitive").asText()).isEqualTo("***REDACTED***");
      Assertions.assertThat(json.get("publicValue").asText()).isEqualTo(expected(expectedPublic, "pub"));
      Assertions.assertThat(json.get("internalValue").asText()).isEqualTo(expected(expectedInternal, "int"));
      Assertions.assertThat(json.get("confidentialValue").asText()).isEqualTo(expected(expectedConfidential, "conf"));
      Assertions.assertThat(json.has("plain")).isFalse();
   }

   @Test
   void shouldMaskClassifiedComponentsOfNestedObjectsAndWholeCollections() throws Exception {
      ObjectMapper mapper = mapperWith(true, true, false);
      String json = mapper.writeValueAsString(new Wrapper(sample(), List.of("a", "b")));
      ((AbstractStringAssert)Assertions.assertThat(json).contains(new CharSequence[]{"\"sensitive\":\"***REDACTED***\"", "\"tags\":\"***REDACTED***\""})).doesNotContain(new CharSequence[]{"secret", "\"a\""});
   }

   @Test
   void shouldKeepAClassifiedNullValueAsNullWhenPseudonymizing() throws Exception {
      ObjectMapper mapper = mapperWith(true, true, false);
      String json = mapper.writeValueAsString(new NullConfidential((String)null));
      Assertions.assertThat(json).isEqualTo("{\"document\":null}");
   }

   @Test
   void shouldSerializeMapsWithoutFailing() throws Exception {
      ObjectMapper mapper = mapperWith(true, true, false);
      String json = mapper.writeValueAsString(Map.of("k", "v"));
      Assertions.assertThat(json).isEqualTo("{\"k\":\"v\"}");
   }

   @Test
   void shouldDropAPropertyWriterWithoutClassification() {
      ClassificationBeanSerializerModifier modifier = new ClassificationBeanSerializerModifier(propertiesWith(true, true, false), new Pseudonymizer("salt"));
      BeanPropertyWriter writer = (BeanPropertyWriter)Mockito.mock(BeanPropertyWriter.class);
      List<BeanPropertyWriter> writers = List.of(writer);
      List<BeanPropertyWriter> result = modifier.changeProperties((SerializationConfig)null, (BeanDescription)null, writers);
      Assertions.assertThat(result).isEmpty();
      ((BeanPropertyWriter)Mockito.verify(writer, Mockito.never())).assignSerializer((JsonSerializer)Mockito.any());
   }

   private static String expected(String token, String clearText) {
      if ("PSEUDO".equals(token)) {
         return (new Pseudonymizer("salt")).pseudonymize(clearText);
      } else {
         return "REDACTED".equals(token) ? "***REDACTED***" : clearText;
      }
   }

   private static AuditProperties propertiesWith(boolean publicData, boolean internal, boolean confidential) {
      return AuditPropertiesFixture.withCleartext(publicData, internal, confidential);
   }

   private static ObjectMapper mapperWith(boolean publicData, boolean internal, boolean confidential) {
      JsonMapper mapper = (JsonMapper)JsonMapper.builder().build();
      mapper.registerModule(new ClassificationMaskingModule(propertiesWith(publicData, internal, confidential), new Pseudonymizer("salt")));
      mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      return mapper;
   }

   private static Sample sample() {
      return new Sample("secret", "pub", "int", "conf", "plain");
   }

   static record Sample(@Sensitive String sensitive, @Public String publicValue, @Internal String internalValue, @Confidential String confidentialValue, String plain) {
   }

   static record Wrapper(@Public Sample sample, @Sensitive List<String> tags) {
   }

   static record NullConfidential(@Confidential String document) {
   }
}
