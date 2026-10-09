package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

import io.micrometer.tracing.Tracer;

/**
 * Opens use case spans through Micrometer Tracing. The span inherits the trace of the span in effect (the HTTP
 * server span, a Kafka listener span or an outer use case); without one, a new trace is started.
 */
public class MicrometerUseCaseTracing implements UseCaseTracing {

    private final Tracer tracer;

    public MicrometerUseCaseTracing(final Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public TraceSpan open(final String useCaseName) {
        final var span = tracer.nextSpan().name(useCaseName).start();
        return new MicrometerTraceSpan(span, tracer.withSpan(span));
    }
}
