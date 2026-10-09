package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

/**
 * Span identifiers generated locally, used when Micrometer Tracing is not available. There is nothing to end.
 */
record StandaloneTraceSpan(String traceId, String spanId, String parentSpanId) implements TraceSpan {

    @Override
    public void close() {
        // nothing to release: the identifiers live only in the audit context
    }
}
