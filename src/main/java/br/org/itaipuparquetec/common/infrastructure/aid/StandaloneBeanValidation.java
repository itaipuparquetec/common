package br.org.itaipuparquetec.common.infrastructure.aid;

import jakarta.validation.*;

import java.util.Set;

/**
 * @author Emanuel Victor
 * @version 1.0.0
 * @since 1.0.0, 10/09/2019
 */
public final class StandaloneBeanValidation {

    StandaloneBeanValidation() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Valida o bean em relação as suas anotações (faz isso sem precisar ir até o banco e abrir transação)
     *
     * @param bean {T}
     */
    public static <T> void validate(T bean) {
        final Configuration<?> config = Validation.byDefaultProvider().configure();
        final ValidatorFactory factory = config.buildValidatorFactory();
        final Validator validator = factory.getValidator();
        factory.close();

        final Set<ConstraintViolation<T>> violations = validator.validate(bean);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

    }

}
