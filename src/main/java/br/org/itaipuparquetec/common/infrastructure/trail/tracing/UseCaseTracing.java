package br.org.itaipuparquetec.common.infrastructure.trail.tracing;

/**
 * Opens one span per use case execution, as a child of the span in effect (ADR-032).
 */
public interface UseCaseTracing {

    TraceSpan open(String useCaseName);
}
