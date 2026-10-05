package br.org.itaipuparquetec.common.domain.exceptions;

/**
 * Marker for domain exceptions. Only domain exceptions are recorded in the audit trail; third-party
 * exceptions are never recorded directly (see {@link TechnicalException}).
 */
public interface DomainException {
}
