package io.github.itaipuparquetec.common.domain.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static io.github.itaipuparquetec.common.domain.exceptions.AlreadyExistsFieldException.ALREADY_EXITS_MESSAGE;
import static io.github.itaipuparquetec.common.domain.exceptions.EmptyFieldException.NOT_EMPTY_MESSAGE;
import static io.github.itaipuparquetec.common.domain.exceptions.InvalidFieldException.INVALID_FIELD_MESSAGE;
import static io.github.itaipuparquetec.common.domain.exceptions.LessThanZeroFieldException.LESS_THAN_ZERO_MESSAGE;
import static io.github.itaipuparquetec.common.domain.exceptions.NotFoundRegisterException.NOT_FOUND_REGISTER_MESSAGE;
import static io.github.itaipuparquetec.common.domain.exceptions.NullFieldException.NOT_NULL_MESSAGE;
import static io.github.itaipuparquetec.common.domain.exceptions.TooLargeFieldException.TO_LARGE_FIELD_MESSAGE;
import static io.github.itaipuparquetec.common.domain.exceptions.TooShortFieldException.TO_SHORT_FIELD_MESSAGE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ExceptionBuilderTest {

    @Test
    void mustThrowAnExceptionWhenTheInputIsNull() {
        final var fieldName = "name";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenNull(null, fieldName)
                .thenThrows())
                .isInstanceOf(NullFieldException.class)
                .hasMessage(NOT_NULL_MESSAGE.formatted(fieldName));
    }

    @Test
    void mustThrowAGenericExceptionWhenConditionIsTrue() {
        final var fieldName = "name";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .when(true, fieldName)
                .thenThrows())
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage(INVALID_FIELD_MESSAGE.formatted(fieldName));
    }

    @Test
    void mustThrowAnAlreadyExistsFieldExceptionWhenConditionIsTrue() {
        final var fieldName = "name";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenAlreadyExists(true, fieldName)
                .thenThrows())
                .isInstanceOf(AlreadyExistsFieldException.class)
                .hasMessage(ALREADY_EXITS_MESSAGE.formatted(fieldName));
    }

    @Test
    void cannotThrowAnAlreadyExistsFieldExceptionWhenConditionIsFalse() {
        final var fieldName = "name";

        assertThatCode(() -> new ExceptionBuilder()
                .whenAlreadyExists(false, fieldName)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @Test
    void mustThrowANotFoundRegisterExceptionWhenConditionIsTrue() {
        final var fieldName = "name";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenNotFoundRegister(true, fieldName)
                .thenThrows())
                .isInstanceOf(NotFoundRegisterException.class)
                .hasMessage(NOT_FOUND_REGISTER_MESSAGE.formatted(fieldName));
    }

    @Test
    void cannotThrowANotFoundRegisterExceptionWhenConditionIsFalse() {
        final var fieldName = "name";

        assertThatCode(() -> new ExceptionBuilder()
                .whenNotFoundRegister(false, fieldName)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @Test
    void cannotThrowAGenericExceptionWhenConditionIsFalse() {
        final var fieldName = "name";

        assertThatCode(() -> new ExceptionBuilder()
                .when(false, fieldName)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @Test
    void cannotThrowAnExceptionWhenTheInputIsNotNull() {
        assertThatCode(() -> new ExceptionBuilder()
                .whenNull("non null input", "name")
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @Test
    void cannotThrowAnExceptionWhenTheInputIsNull() {
        assertThatCode(() -> new ExceptionBuilder()
                .whenEmpty(null, "age")
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    void mustThrowAnExceptionWhenTheInputIsEmpty(final String input) {
        final var fieldName = "name";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenEmpty(input, fieldName)
                .thenThrows())
                .isInstanceOf(EmptyFieldException.class)
                .hasMessage(NOT_EMPTY_MESSAGE.formatted(fieldName));
    }

    @Test
    void mustThrowNotNullExceptionWhenTheInputIsNullOrEmpty() {

        final var fieldName = "name";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenNullOrEmpty(null, fieldName)
                .thenThrows())
                .isInstanceOf(NullFieldException.class)
                .hasMessage(NOT_NULL_MESSAGE.formatted(fieldName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    void mustThrowNotEmptyExceptionWhenTheInputIsNullOrEmpty(final String input) {

        final var fieldName = "name";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenNullOrEmpty(input, fieldName)
                .thenThrows())
                .isInstanceOf(EmptyFieldException.class)
                .hasMessage(NOT_EMPTY_MESSAGE.formatted(fieldName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"value", " value", " value ", "value "})
    void cannotThrowAnyExceptionWhenTheInputIsNotNullAndIsNotEmpty(final String input) {

        final var fieldName = "name";

        assertThatCode(() -> new ExceptionBuilder()
                .whenNullOrEmpty(input, fieldName)
                .thenThrows()).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", " 1 ", "1  ", "1  1", "  1", "valid input"})
    void cannotThrowAnExceptionWhenTheInputIsNotEmpty(final String input) {
        final var fieldName = "name";

        assertThatCode(() -> new ExceptionBuilder()
                .whenEmpty(input, fieldName)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -2, -15, Integer.MAX_VALUE * -1})
    void mustThrowAnExceptionWhenTheInputIsLessThanZero(int value) {
        final var field = "age";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenLessThanZero(value, field)
                .thenThrows())
                .isInstanceOf(LessThanZeroFieldException.class)
                .hasMessage(LESS_THAN_ZERO_MESSAGE.formatted(field));
    }

    @Test
    void mustNotThrowAnyExceptionWhenValidatingLessThanZeroWithNullValue() {
        final var field = "age";

        assertThatCode(() -> new ExceptionBuilder()
                .whenLessThanZero(null, field)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 15, Integer.MAX_VALUE})
    void cannotThrowAnExceptionWhenTheInputIsEqualsOrGreaterThanZero(int value) {
        assertThatCode(() -> new ExceptionBuilder()
                .whenLessThanZero(value, "age")
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("provideNumberGreaterThan")
    void mustThrowAnExceptionWhenTheInputIsGreaterThan(int value, int maxSize) {
        final var field = "age";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenTheNumberIsGreaterThan(value, maxSize, field)
                .thenThrows())
                .isInstanceOf(TooLargeFieldException.class)
                .hasMessage(TO_LARGE_FIELD_MESSAGE.formatted(field, maxSize));
    }

    @Test
    void mustNotThrowAnyExceptionWhenValidatingTheNumberIsGreaterThanWithNullValue() {
        final var field = "age";
        final int max = 1;

        assertThatCode(() -> new ExceptionBuilder()
                .whenTheNumberIsGreaterThan(null, max, field)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("provideNumberGreaterThan")
    void cannotThrowAnExceptionWhenTheInputIsEqualsOrLessThanMaxSize(int maxSize, int value) {
        final var field = "age";

        assertThatCode(() -> new ExceptionBuilder()
                .whenTheNumberIsGreaterThan(value, maxSize, field)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    @Test
    void mustNotThrowAnyExceptionWhenValidatingTheNumberIsLessThanWithNullValue() {
        final var field = "age";
        final int min = 0;

        assertThatCode(() -> new ExceptionBuilder()
                .whenTheNumberIsLessThan(null, min, field)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideNumberGreaterThan() {
        return Stream.of(
                Arguments.of(15, 10),
                Arguments.of(2, 1),
                Arguments.of(1, 0),
                Arguments.of(3, -5)
        );
    }

    @ParameterizedTest
    @MethodSource("provideNumberLessThan")
    void mustThrowAnExceptionWhenTheInputIsLessThan(int value, int minSize) {
        final var field = "age";

        assertThatThrownBy(() -> new ExceptionBuilder()
                .whenTheNumberIsLessThan(value, minSize, field)
                .thenThrows())
                .isInstanceOf(TooShortFieldException.class)
                .hasMessage(TO_SHORT_FIELD_MESSAGE.formatted(field, minSize));
    }

    @ParameterizedTest
    @MethodSource("provideNumberLessThan")
    void cannotThrowAnExceptionWhenTheInputIsEqualsOrGreaterThanMaxSize(int minSize, int value) {
        final var field = "age";

        assertThatCode(() -> new ExceptionBuilder()
                .whenTheNumberIsLessThan(value, minSize, field)
                .thenThrows())
                .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideNumberLessThan() {
        return Stream.of(
                Arguments.of(10, 15),
                Arguments.of(1, 2),
                Arguments.of(0, 1),
                Arguments.of(-5, 3),
                Arguments.of(-5, -2)
        );
    }
}
