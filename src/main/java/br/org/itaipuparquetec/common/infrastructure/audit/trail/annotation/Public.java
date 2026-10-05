package br.org.itaipuparquetec.common.infrastructure.audit.trail.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a data element as <b>Public</b> per ADR-021: free disclosure, no risk.
 * Public data is written to the audit trail in clear text (subject to {@code audit.cleartext.public}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Public {
}
