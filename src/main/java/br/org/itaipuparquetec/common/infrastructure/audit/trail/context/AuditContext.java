package br.org.itaipuparquetec.common.infrastructure.audit.trail.context;

import org.slf4j.MDC;

/**
 * Context identifiers kept in the MDC and propagated across the threads of a use case. Input and output bodies
 * never go through the MDC: the aspect receives them directly and includes them in the event.
 */
public final class AuditContext {

    public static final String ACTOR = "actor";
    public static final String SID = "sid";
    public static final String JTI = "jti";
    public static final String TRACE_ID = "traceId";

    private AuditContext() {
    }

    public static void put(final String actor, final String sid, final String jti, final String traceId) {
        putWhenPresent(ACTOR, actor);
        putWhenPresent(SID, sid);
        putWhenPresent(JTI, jti);
        putWhenPresent(TRACE_ID, traceId);
    }

    public static void clear() {
        MDC.remove(ACTOR);
        MDC.remove(SID);
        MDC.remove(JTI);
        MDC.remove(TRACE_ID);
    }

    private static void putWhenPresent(final String key, final String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
