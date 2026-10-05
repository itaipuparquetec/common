package br.org.itaipuparquetec.common.domain.exceptions;

/**
 * Generic domain exception that wraps technical failures raised by any adapter (database, REST call to another
 * microservice, etc.). It carries no business payload, since the original error may contain sensitive data.
 * The audit trail records only that a technical error occurred; the root cause is investigated in the generic
 * observability dashboard, located by the {@code traceId}.
 */
public class TechnicalException extends RuntimeException implements DomainException {

    public TechnicalException(final String message) {
        super(message);
    }

    public TechnicalException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
