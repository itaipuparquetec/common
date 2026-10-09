package br.org.itaipuparquetec.common.infrastructure.trail.identity;

/**
 * Identifies the service that produces the audit events.
 *
 * @param service the {@code spring.application.name}
 * @param version version of the producing artifact, or {@code null} when unknown
 */
public record SourceIdentity(String service, String version) {
}
