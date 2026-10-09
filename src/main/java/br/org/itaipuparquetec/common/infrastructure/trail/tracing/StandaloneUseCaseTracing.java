package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

import br.org.itaipuparquetec.common.infrastructure.trail.context.AuditContext;
import org.slf4j.MDC;

import java.util.HexFormat;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * Fallback used when no Micrometer {@code Tracer} is available. It reuses a W3C trace id found in the MDC (making
 * the current span id the parent) or starts a new W3C-formatted trace.
 */
public class StandaloneUseCaseTracing implements UseCaseTracing {

    private static final Pattern W3C_TRACE_ID = Pattern.compile("[0-9a-f]{32}");
    private static final int TRACE_ID_BYTES = 16;
    private static final int SPAN_ID_BYTES = 8;

    private final RandomGenerator random;

    public StandaloneUseCaseTracing(final RandomGenerator random) {
        this.random = random;
    }

    @Override
    public TraceSpan open(final String useCaseName) {
        final var current = MDC.get(AuditContext.TRACE_ID);
        if (current != null && W3C_TRACE_ID.matcher(current).matches()) {
            return new StandaloneTraceSpan(current, randomHex(SPAN_ID_BYTES), MDC.get(AuditContext.SPAN_ID));
        }
        return new StandaloneTraceSpan(randomHex(TRACE_ID_BYTES), randomHex(SPAN_ID_BYTES), null);
    }

    private String randomHex(final int byteCount) {
        final var bytes = new byte[byteCount];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
