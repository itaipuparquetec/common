package br.org.itaipuparquetec.common.infrastructure.trail;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a read-only use case in to the audit trail. Executions inside a read-only transaction are skipped unless
 * {@code audit.include-read-only=true} or the use case carries this annotation (e.g. queries over sensitive
 * Guardian data).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AuditSensitiveRead {
}
