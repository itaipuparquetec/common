package br.org.itaipuparquetec.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Utilize to ignore some methods in arch unit function.
 * You need to create a technical-debt every time you use it.
 */
@Target(ElementType.METHOD)
public @interface ArchUnitMethodIgnore {
}
