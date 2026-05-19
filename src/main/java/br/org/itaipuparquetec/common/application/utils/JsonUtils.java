package br.org.itaipuparquetec.common.application.utils;

import br.org.itaipuparquetec.common.domain.exceptions.InvalidFieldException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new InvalidFieldException("Invalid obj: " + obj );
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new InvalidFieldException("Invalid JSON: " + json );
        }
    }
}
