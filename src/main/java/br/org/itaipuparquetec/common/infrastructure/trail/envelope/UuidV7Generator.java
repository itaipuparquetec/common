package br.org.itaipuparquetec.common.infrastructure.trail.envelope;

import java.time.Clock;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Generates time-ordered UUIDs (version 7, RFC 9562): 48 bits of Unix milliseconds followed by random bits.
 */
public class UuidV7Generator {

    private static final long VERSION_7 = 0x7000L;
    private static final long RANDOM_A_MASK = 0x0FFFL;
    private static final long RANDOM_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANT_RFC_4122 = 0x8000_0000_0000_0000L;
    private static final int TIMESTAMP_SHIFT = 16;

    private final Clock clock;
    private final RandomGenerator random;

    public UuidV7Generator(final Clock clock, final RandomGenerator random) {
        this.clock = clock;
        this.random = random;
    }

    public UUID next() {
        final var mostSignificantBits = (clock.millis() << TIMESTAMP_SHIFT)
                | VERSION_7
                | (random.nextLong() & RANDOM_A_MASK);
        final var leastSignificantBits = (random.nextLong() & RANDOM_B_MASK) | VARIANT_RFC_4122;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
