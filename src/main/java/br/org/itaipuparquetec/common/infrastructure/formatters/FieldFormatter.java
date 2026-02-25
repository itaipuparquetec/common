package br.org.itaipuparquetec.common.infrastructure.formatters;

import java.util.StringJoiner;

public final class FieldFormatter {

    FieldFormatter() {
        throw new IllegalStateException("Utility class");
    }

    public static String format(String conjunction, String... fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }

        if (fields.length == 1) {
            return fields[0];
        }

        if (fields.length == 2) {
            return fields[0] + " " + conjunction + " " + fields[1];
        }

        final var joiner = new StringJoiner(", ");
        for (int i = 0; i < fields.length - 1; i++) {
            joiner.add(fields[i]);
        }

        return joiner + " " + conjunction + " " + fields[fields.length - 1];
    }

}
