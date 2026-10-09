package br.org.itaipuparquetec.common.infrastructure.trail.envelope;

import br.org.itaipuparquetec.common.infrastructure.trail.AuditEventFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.AuditPropertiesFixture;
import br.org.itaipuparquetec.common.infrastructure.trail.event.AuditEvent;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.AuditMappers;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.Pseudonymizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.IterableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TrailEventEnvelopeContractTest {
   private final ObjectMapper mapper = AuditMappers.envelope();
   private final TrailEventEnvelopeFactory factory = new TrailEventEnvelopeFactory(AuditMappers.masking(AuditPropertiesFixture.withCleartext(true, true, false), new Pseudonymizer("salt")), 32768);

   TrailEventEnvelopeContractTest() {
   }

   @ParameterizedTest
   @ValueSource(
      booleans = {true, false}
   )
   void shouldProduceEnvelopesThatSatisfyTheV1Schema(boolean success) throws IOException {
      AuditEvent event = success ? AuditEventFixture.successWithInput("InsertNewItemUseCase", "payload") : AuditEventFixture.failureOf("InsertNewItemUseCase");
      JsonNode json = this.mapper.valueToTree(this.factory.envelopeOf(event));
      List<String> violations = violationsOf(json, this.schema());
      Assertions.assertThat(violations).isEmpty();
   }

   @Test
   void shouldRequireTheErrorWhenTheResultIsNotSuccess() throws IOException {
      ObjectNode json = (ObjectNode)this.mapper.valueToTree(this.factory.envelopeOf(AuditEventFixture.failureOf("Register")));
      json.remove("error");
      List<String> violations = violationsOf(json, this.schema());
      Assertions.assertThat(violations).contains(new String[]{"error is required when result is not SUCCESS"});
   }

   @Test
   void shouldDeclareNeitherCollectionsNorTheUseCaseOutputNorTheErrorMessage() throws IOException {
      JsonNode properties = this.schema().get("properties");
      ((IterableAssert)Assertions.assertThat(properties.fieldNames()).toIterable().doesNotContain(new String[]{"output", "message", "errorMessage"})).allSatisfy(name -> Assertions.assertThat(properties.get((String)name).get("type").asText()).isNotEqualTo("array"));
   }

   private JsonNode schema() throws IOException {
      InputStream stream = this.getClass().getResourceAsStream("/trail/trail-event-v1.schema.json");

      JsonNode var2;
      try {
         var2 = this.mapper.readTree(stream);
      } catch (Throwable var5) {
         if (stream != null) {
            try {
               stream.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stream != null) {
         stream.close();
      }

      return var2;
   }

   private static List<String> violationsOf(JsonNode json, JsonNode schema) {
      List<String> violations = new ArrayList();
      JsonNode properties = schema.get("properties");
      schema.get("required").forEach(name -> {
         if (!json.has(name.asText())) {
            violations.add(name.asText() + " is required");
         }

      });
      json.fieldNames().forEachRemaining(name -> violations.addAll(violationsOfField(name, json, properties)));
      if (!"SUCCESS".equals(json.get("result").asText()) && !json.has("error")) {
         violations.add("error is required when result is not SUCCESS");
      }

      return violations;
   }

   private static List<String> violationsOfField(String name, JsonNode json, JsonNode properties) {
      JsonNode definition = properties.get(name);
      if (definition == null) {
         return List.of(name + " is not declared by the schema");
      } else {
         JsonNode value = json.get(name);
         List<String> violations = new ArrayList();
         if (definition.has("pattern") && !value.asText().matches(definition.get("pattern").asText())) {
            violations.add(name + " does not match its pattern");
         }

         if (definition.has("const") && !value.asText().equals(definition.get("const").asText())) {
            violations.add(name + " differs from its constant");
         }

         if (definition.has("enum") && !contains(definition.get("enum"), value.asText())) {
            violations.add(name + " is not one of its enum values");
         }

         return violations;
      }
   }

   private static boolean contains(JsonNode values, String candidate) {
      for(JsonNode value : values) {
         if (value.asText().equals(candidate)) {
            return true;
         }
      }

      return false;
   }
}
