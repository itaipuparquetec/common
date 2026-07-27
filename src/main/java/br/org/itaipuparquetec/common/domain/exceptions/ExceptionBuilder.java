package br.org.itaipuparquetec.common.domain.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExceptionBuilder {

    private final List<RuntimeException> exceptions = new ArrayList<>();

    public ExceptionBuilder whenNull(final Object value, final String field) {
        if (value == null) {
            this.exceptions.add(new NullFieldException(field));
        }
        return this;
    }

    public ExceptionBuilder whenEmpty(final String value, final String field) {
        if (Objects.nonNull(value) && (value.isEmpty() || value.trim().isEmpty())) {
            this.exceptions.add(new EmptyFieldException(field));
        }
        return this;
    }

    public ExceptionBuilder whenNullOrEmpty(final Object value, final String field) {
        if (value == null) {
            this.exceptions.add(new NullFieldException(field));
            return this;
        }
        if ((value.toString().isEmpty() || value.toString().trim().isEmpty())) {
            this.exceptions.add(new EmptyFieldException(field));
        }
        return this;
    }

    public ExceptionBuilder whenTheNumberIsLessThan(final Integer value, final int minSize, final String field) {
        if (value != null && value < minSize) {
            this.exceptions.add(new TooShortFieldException(field, minSize));
        }
        return this;
    }

    public ExceptionBuilder whenTheNumberIsGreaterThan(final Integer value, final int maxSize, final String field) {
        if (value != null && value > maxSize) {
            this.exceptions.add(new TooLargeFieldException(field, maxSize));
        }
        return this;
    }

    public ExceptionBuilder whenStringSizeGreaterThan(final String value, final int maxSize, final String field) {
        if (value != null && value.length() > maxSize) {
            this.exceptions.add(new TooLargeFieldException(field, maxSize));
        }
        return this;
    }

    public ExceptionBuilder whenStringSizeLessThan(final String value, final int maxSize, final String field) {
        if (value != null && value.length() < maxSize) {
            this.exceptions.add(new TooShortFieldException(field, maxSize));
        }
        return this;
    }

    public ExceptionBuilder whenLessThanZero(final Integer value, final String field) {
        if (value != null && value < 0) {
            this.exceptions.add(new LessThanZeroFieldException(field));
        }
        return this;
    }

    public ExceptionBuilder whenAlreadyExists(final boolean condition, final String field) {
        if (condition) {
            this.exceptions.add(new AlreadyExistsFieldException(field));
        }
        return this;
    }

    public ExceptionBuilder whenAlreadyExists(final boolean condition, final String... fields) {
        if (condition) {
            this.exceptions.add(new AlreadyExistsFieldsException(fields));
        }
        return this;
    }

    public ExceptionBuilder whenNotFoundRegister(final boolean condition, final String field) {
        if (condition) {
            this.exceptions.add(new NotFoundRegisterException(field));
        }
        return this;
    }

    public ExceptionBuilder whenForbiddenAccess(final boolean condition, final String field) {
        if (condition) {
            this.exceptions.add(new ForbiddenException(field));
        }
        return this;
    }

    public ExceptionBuilder when(final boolean condition, final String field) {
        if (condition) {
            this.exceptions.add(new InvalidFieldException(field));
        }
        return this;
    }

    public void thenThrows() {
        if (!exceptions.isEmpty()) {
            throw exceptions.getFirst();
        }
    }
}
