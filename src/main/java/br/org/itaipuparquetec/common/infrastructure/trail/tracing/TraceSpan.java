package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

/**
 * Span of a use case execution. Closing it ends the span and restores the previous tracing context.
 */
public interface TraceSpan extends AutoCloseable {

    /** The 32 lowercase hex characters W3C trace id. */
    String traceId();

    /** The 16 hex characters id of this span. */
    String spanId();

    /** The id of the parent span, or {@code null} when this span is the root of the trace. */
    String parentSpanId();

    @Override
    void close();
}
