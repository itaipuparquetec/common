package br.org.itaipuparquetec.common.application.services;

public interface JsonService {

    String toJson(Object obj);

    <T> T fromJson(String json, Class<T> clazz);
}
