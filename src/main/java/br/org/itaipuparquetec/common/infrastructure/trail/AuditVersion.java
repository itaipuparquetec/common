package br.org.itaipuparquetec.common.infrastructure.trail;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the version of a use case, recorded in every audit event. Defaults to {@code "1"} when absent.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AuditVersion {
    String value();
}
