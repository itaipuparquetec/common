package br.org.itaipuparquetec.common.infrastructure.services;

import br.org.itaipuparquetec.common.application.services.JsonService;
import br.org.itaipuparquetec.common.domain.exceptions.InvalidFieldException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JsonServiceImpl implements JsonService {

    private final ObjectMapper objectMapper;

    @Override
    public String toJson(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new InvalidFieldException("Invalid obj: " + obj);
        }
    }

    @Override
    public <T> T fromJson(final String json, final Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new InvalidFieldException("Invalid JSON: " + json);
        }
    }
}
