package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * {@link TraceSpan} backed by a Micrometer Tracing span made current for the executing thread.
 */
final class MicrometerTraceSpan implements TraceSpan {

    private final Span span;
    private final Tracer.SpanInScope scope;

    MicrometerTraceSpan(final Span span, final Tracer.SpanInScope scope) {
        this.span = span;
        this.scope = scope;
    }

    @Override
    public String traceId() {
        return span.context().traceId();
    }

    @Override
    public String spanId() {
        return span.context().spanId();
    }

    @Override
    public String parentSpanId() {
        final var parentId = span.context().parentId();
        return parentId == null || parentId.isBlank() ? null : parentId;
    }

    @Override
    public void close() {
        scope.close();
        span.end();
    }
}
