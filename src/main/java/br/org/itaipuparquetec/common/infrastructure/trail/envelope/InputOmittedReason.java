package br.org.itaipuparquetec.common.infrastructure.trail.envelope;

/**
 * Why the {@code input} of an event was left out of the envelope.
 */
public enum InputOmittedReason {
    /** The masked input exceeded {@code audit.max-input-bytes}; truncating would produce invalid JSON. */
    SIZE_LIMIT,
    /** The input could not be serialized. */
    SERIALIZATION_ERROR
}
