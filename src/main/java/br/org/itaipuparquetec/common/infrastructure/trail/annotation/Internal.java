package br.org.itaipuparquetec.common.infrastructure.trail.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a data element as <b>Internal</b> per ADR-021: corporate use, no personal data.
 * Internal data is written to the audit trail in clear text (subject to {@code audit.cleartext.internal}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Internal {
}
