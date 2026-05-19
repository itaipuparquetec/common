package br.org.itaipuparquetec.common.application.utils;

import br.org.itaipuparquetec.common.domain.exceptions.InvalidFieldException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonUtils {
    private final ObjectMapper objectMapper;


    @Autowired
    public JsonUtils(ObjectMapper mapper) {
        this.objectMapper = mapper;
    }

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new InvalidFieldException("Invalid obj: " + obj );
        }
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new InvalidFieldException("Invalid JSON: " + json );
        }
    }
}
