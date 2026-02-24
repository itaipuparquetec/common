package br.org.itaipuparquetec.common.infrastructure.i18n;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageSourceHolderTest {

    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        MessageSourceHolder.setMessageSource(messageSource);
    }

    @Test
    void cannotInstantiateMessageSourceHolderByDefaultConstructor() {
        final var exception = assertThrows(IllegalStateException.class, MessageSourceHolder::new);

        assertThat(exception.getMessage()).isEqualTo("Utility class");
    }

    @Test
    void shouldReturnMessageForGivenCode() {
        Locale locale = Locale.ENGLISH;
        LocaleContextHolder.setLocale(locale);
        String code = "test.code";
        String expected = "Test Message";
        when(messageSource.getMessage(eq(code), isNull(), eq(code), eq(locale))).thenReturn(expected);

        String result = MessageSourceHolder.getMessage(code);

        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnMessageWithArgumentsForLocaleMethod() {
        Locale locale = Locale.ENGLISH;
        String code = "test.code.args";
        Object[] args = new Object[]{"a", 1};
        String expected = "Message with args";
        when(messageSource.getMessage(code, args, locale)).thenReturn(expected);

        String result = MessageSourceHolder.getMessage(locale, code, args);

        Assertions.assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnDefaultMessageWhenNotFound() {
        Locale locale = Locale.ENGLISH;
        LocaleContextHolder.setLocale(locale);
        String code = "not.found";
        String defaultMessage = "Default Message";
        when(messageSource.getMessage(eq(code), isNull(), eq(defaultMessage), eq(locale))).thenReturn(defaultMessage);

        String result = MessageSourceHolder.getMessage(code, defaultMessage);

        Assertions.assertThat(result).isEqualTo(defaultMessage);
    }

    @Test
    void shouldThrowWhenMessageSourceNotSet() {
        MessageSourceHolder.setMessageSource(null);

        assertThrows(IllegalArgumentException.class, () -> MessageSourceHolder.getMessage(Locale.ENGLISH, "any.code"));
    }

    @Test
    void shouldReturnMessageForResolvable() {
        MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
        Locale locale = Locale.ENGLISH;
        String expected = "Resolvable Message";
        when(messageSource.getMessage(resolvable, locale)).thenReturn(expected);

        String result = MessageSourceHolder.getMessage(resolvable, locale);

        Assertions.assertThat(result).isEqualTo(expected);
    }
}
