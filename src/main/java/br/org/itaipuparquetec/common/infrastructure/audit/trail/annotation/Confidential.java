package br.org.itaipuparquetec.common.infrastructure.audit.trail.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a data element as <b>Confidential</b> per ADR-021: common personal data (LGPD Art. 5, I) or
 * restricted business information. By default it is pseudonymized in the audit trail; it is only written in
 * clear text when {@code audit.cleartext.confidential} is enabled.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Confidential {
}
