package io.github.itaipuparquetec.common.domain.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExceptionBuilder {

    private final List<RuntimeException> exceptions = new ArrayList<>();

    public ExceptionBuilder() {
    }

    public ExceptionBuilder whenNull(Object value, String field) {
        if (value == null)
            this.exceptions.add(new NullFieldException(field));
        return this;
    }

    public ExceptionBuilder whenEmpty(String value, String field) {
        if (Objects.nonNull(value) && (value.isEmpty() || value.trim().isEmpty()))
            this.exceptions.add(new EmptyFieldException(field));
        return this;
    }

    public ExceptionBuilder whenNullOrEmpty(Object value, String field) {
        if (value == null) {
            this.exceptions.add(new NullFieldException(field));
            return this;
        }
        if ((value.toString().isEmpty() || value.toString().trim().isEmpty()))
            this.exceptions.add(new EmptyFieldException(field));
        return this;
    }

    public ExceptionBuilder whenTheNumberIsLessThan(Integer value, int minSize, String field) {
        if (value != null && value < minSize)
            this.exceptions.add(new TooShortFieldException(field, minSize));
        return this;
    }

    public ExceptionBuilder whenTheNumberIsGreaterThan(Integer value, int maxSize, String field) {
        if (value != null && value > maxSize)
            this.exceptions.add(new TooLargeFieldException(field, maxSize));
        return this;
    }

    public ExceptionBuilder whenLessThanZero(Integer value, String field) {
        if (value != null && value < 0)
            this.exceptions.add(new LessThanZeroFieldException(field));
        return this;
    }

    public ExceptionBuilder whenAlreadyExists(boolean condition, String field) {
        if (condition)
            this.exceptions.add(new AlreadyExistsFieldException(field));
        return this;
    }

    public ExceptionBuilder whenNotFoundRegister(boolean condition, String field) {
        if (condition)
            this.exceptions.add(new NotFoundRegisterException(field));
        return this;
    }

    public ExceptionBuilder when(boolean condicao, String field) {
        if (condicao)
            this.exceptions.add(new InvalidFieldException(field));
        return this;
    }

    public void thenThrows() {
        if (!exceptions.isEmpty()) {
            throw exceptions.getFirst();
        }
    }
}