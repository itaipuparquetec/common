package br.org.itaipuparquetec.common.application.utils;

import br.org.itaipuparquetec.common.domain.exceptions.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilsTest {

    @Test
    void toJson_shouldConvertObjectToJsonString() {
        Map<String, String> map = Map.of("nome", "Ana", "idade", "30");

        final var json = JsonUtils.toJson(map);

        assertThat(json)
                .contains("\"nome\":\"Ana\"")
                .contains("\"idade\":\"30\"");
    }

    @Test
    void fromJson_shouldConvertJsonStringToObject() {
        String json = "{\"nome\":\"Ana\",\"idade\":\"30\"}";
        Map<String, Object> map = JsonUtils.fromJson(json, Map.class);

        assertThat(map).containsEntry("nome", "Ana").containsEntry("idade", "30");
    }

    @Test
    void toJson_shouldThrowRuntimeExceptionOnInvalidObject() {
        Object invalidObj = new Object() {};

        assertThatThrownBy(() -> JsonUtils.toJson(invalidObj))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void fromJson_shouldThrowRuntimeExceptionOnInvalidJson() {
        String invalidJson = "{nome:\"Ana\"";

        assertThatThrownBy(() -> JsonUtils.fromJson(invalidJson, Map.class))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void toJsonAndFromJson_shouldBeInverseOperations() {
        Map<String, String> original = Map.of("key", "value");

        String json = JsonUtils.toJson(original);
        Map<String, Object> result = JsonUtils.fromJson(json, Map.class);

        assertThat(result).isEqualTo(original);
    }
}