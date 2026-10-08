package br.org.itaipuparquetec.common.infrastructure.trail.context;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context identifiers kept in the MDC and propagated across the threads of a use case. Input bodies never go
 * through the MDC: the aspect receives them directly and includes them in the event.
 *
 * <p>Opening the context returns a scope that restores the values present before, so a nested use case never
 * erases the context of the use case that called it.</p>
 */
public final class AuditContext {

    public static final String ACTOR = "actor";
    public static final String SID = "sid";
    public static final String JTI = "jti";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";

    private static final List<String> KEYS = List.of(ACTOR, SID, JTI, TRACE_ID, SPAN_ID);

    private AuditContext() {
    }

    public static AuditContextScope open(final String actor, final String sid, final String jti,
                                         final String traceId, final String spanId) {
        final var scope = new AuditContextScope(currentValues());
        putWhenPresent(ACTOR, actor);
        putWhenPresent(SID, sid);
        putWhenPresent(JTI, jti);
        putWhenPresent(TRACE_ID, traceId);
        putWhenPresent(SPAN_ID, spanId);
        return scope;
    }

    private static Map<String, String> currentValues() {
        final Map<String, String> values = new HashMap<>();
        KEYS.forEach(key -> values.put(key, MDC.get(key)));
        return values;
    }

    private static void putWhenPresent(final String key, final String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
