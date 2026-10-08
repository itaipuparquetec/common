package br.org.itaipuparquetec.common.infrastructure.trail.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a data element as <b>Restricted / Sensitive</b> per ADR-021: sensitive personal data (LGPD Art. 5, II),
 * secrets and data whose leak poses relevant risk to the subject (e.g. Guardian whistleblowing reports).
 * Sensitive data is <b>never</b> written to the audit trail in clear text; it is always redacted.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface Sensitive {
}
