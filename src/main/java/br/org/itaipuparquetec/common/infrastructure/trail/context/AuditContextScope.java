package br.org.itaipuparquetec.common.infrastructure.trail.context;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Remembers the MDC values that existed before an {@link AuditContext} was opened and puts them back on close.
 */
public final class AuditContextScope implements AutoCloseable {

    private final Map<String, String> previousValues;

    AuditContextScope(final Map<String, String> previousValues) {
        this.previousValues = previousValues;
    }

    @Override
    public void close() {
        previousValues.forEach(AuditContextScope::restore);
    }

    private static void restore(final String key, final String previousValue) {
        if (previousValue == null) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, previousValue);
    }
}
