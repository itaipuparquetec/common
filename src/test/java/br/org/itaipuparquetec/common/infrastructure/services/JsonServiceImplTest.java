package br.org.itaipuparquetec.common.infrastructure.services;

import br.org.itaipuparquetec.common.application.services.JsonService;
import br.org.itaipuparquetec.common.domain.exceptions.InvalidFieldException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonServiceImplTest {

    private JsonService jsonUtils;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        jsonUtils = new JsonServiceImpl(objectMapper);
    }

    @Test
    void shouldConvertObjectToJsonString() {
        final var map = Map.of("nome", "Ana", "idade", "30");

        final String json = jsonUtils.toJson(map);

        assertThat(json)
                .contains("\"nome\":\"Ana\"")
                .contains("\"idade\":\"30\"");
    }

    @Test
    void shouldConvertJsonStringToObject() {
        String json = "{\"nome\":\"Ana\",\"idade\":\"30\"}";

        final var map = jsonUtils.fromJson(json, Map.class);

        assertThat(map).containsEntry("nome", "Ana").containsEntry("idade", "30");
    }

    @Test
    void shouldThrowRuntimeExceptionOnInvalidObject() {
        Object invalidObj = new Object() {};

        assertThatThrownBy(() -> jsonUtils.toJson(invalidObj)).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void shouldThrowRuntimeExceptionOnInvalidJson() {
        String invalidJson = "{nome:\"Ana\"";

        assertThatThrownBy(() -> jsonUtils.fromJson(invalidJson, Map.class)).isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void shouldBeInverseOperations() {
        final var original = Map.of("key", "value");

        final var json = jsonUtils.toJson(original);
        final var result = jsonUtils.fromJson(json, Map.class);

        assertThat(result).containsEntry("key", "value");
    }
}