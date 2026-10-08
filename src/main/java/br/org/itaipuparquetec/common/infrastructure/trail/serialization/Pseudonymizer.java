package br.org.itaipuparquetec.common.infrastructure.trail.serialization;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Produces a deterministic, irreversible pseudonym for a value, so the same input always maps to the same
 * pseudonym (allowing correlation) without exposing the original data of third parties.
 */
public class Pseudonymizer {

    private static final String ALGORITHM = "SHA-256";
    private static final String PREFIX = "anon:";
    private static final int PSEUDONYM_LENGTH = 16;

    private final String salt;

    public Pseudonymizer(final String salt) {
        this.salt = salt == null ? "" : salt;
    }

    public String pseudonymize(final Object value) {
        if (value == null) {
            return null;
        }
        return PREFIX + hash(salt + value).substring(0, PSEUDONYM_LENGTH);
    }

    private static String hash(final String value) {
        try {
            final var digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing digest algorithm: " + ALGORITHM, exception);
        }
    }
}
