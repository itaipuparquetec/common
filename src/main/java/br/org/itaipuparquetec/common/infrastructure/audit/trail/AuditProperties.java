package br.org.itaipuparquetec.common.infrastructure.audit.trail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

/**
 * Audit trail configuration, bound from the environment (e.g. via {@code .env}).
 *
 * <p>Sensitive data is never rendered in clear text regardless of these flags.</p>
 *
 * <ul>
 *   <li>{@code AUDIT_ENABLED} → {@code audit.enabled}</li>
 *   <li>{@code AUDIT_CLEARTEXT_PUBLIC} → {@code audit.cleartext.public}</li>
 *   <li>{@code AUDIT_CLEARTEXT_INTERNAL} → {@code audit.cleartext.internal}</li>
 *   <li>{@code AUDIT_CLEARTEXT_CONFIDENTIAL} → {@code audit.cleartext.confidential}</li>
 *   <li>{@code AUDIT_PSEUDONYMIZATION_SALT} → {@code audit.pseudonymization-salt}</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "audit")
public record AuditProperties(
        @DefaultValue("true") boolean enabled,
        String pseudonymizationSalt,
        @DefaultValue Cleartext cleartext) {

    /**
     * Per-category clear-text flags. When a category is not allowed in clear text, confidential data is
     * pseudonymized and every other non-public category is redacted.
     */
    public record Cleartext(
            @Name("public") @DefaultValue("true") boolean publicData,
            @DefaultValue("true") boolean internal,
            @DefaultValue("false") boolean confidential) {
    }

    public MaskingStrategy resolveStrategy(final DataClassification classification) {
        return switch (classification) {
            case PUBLIC -> cleartext.publicData() ? MaskingStrategy.NONE : MaskingStrategy.REDACT;
            case INTERNAL -> cleartext.internal() ? MaskingStrategy.NONE : MaskingStrategy.REDACT;
            case CONFIDENTIAL -> cleartext.confidential() ? MaskingStrategy.NONE : MaskingStrategy.PSEUDONYMIZE;
            case SENSITIVE -> MaskingStrategy.REDACT;
        };
    }
}
