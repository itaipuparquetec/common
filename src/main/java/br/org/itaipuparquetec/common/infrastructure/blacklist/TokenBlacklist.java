package br.org.itaipuparquetec.common.infrastructure.blacklist;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.nimbusds.jwt.JWTParser;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * The revoked access tokens, each kept until it expires by itself ({@code exp}): after that the token is refused
 * anyway. No token is kept longer than the longest life of an access token of the Hubti, and a token without
 * expiration, or that is not a JWT, is kept for that long.
 */
@Component
public class TokenBlacklist {

    static final Duration LONGEST_ACCESS_TOKEN_LIFE = Duration.ofHours(1);

    private final Clock clock;
    private final Cache<String, Instant> revokedUntil;

    public TokenBlacklist() {
        this(Clock.systemUTC(), Ticker.systemTicker());
    }

    TokenBlacklist(final Clock clock, final Ticker ticker) {
        this.clock = clock;
        this.revokedUntil = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfter(new UntilTheTokenExpires())
                .build();
    }

    public void revoke(final String token) {
        final var until = expirationOf(token);
        if (until.isAfter(clock.instant())) {
            revokedUntil.put(token, until);
        }
    }

    public boolean isRevoked(final String token) {
        return revokedUntil.getIfPresent(token) != null;
    }

    private Instant expirationOf(final String token) {
        final var longest = clock.instant().plus(LONGEST_ACCESS_TOKEN_LIFE);
        try {
            final var expiration = JWTParser.parse(token).getJWTClaimsSet().getExpirationTime();
            return expiration == null || expiration.toInstant().isAfter(longest) ? longest : expiration.toInstant();
        } catch (final ParseException exception) {
            return longest;
        }
    }

    private final class UntilTheTokenExpires implements Expiry<String, Instant> {

        @Override
        public long expireAfterCreate(final String token, final Instant until, final long currentTime) {
            return Math.max(0, Duration.between(clock.instant(), until).toNanos());
        }

        @Override
        public long expireAfterUpdate(final String token, final Instant until, final long currentTime,
                                      final long currentDuration) {
            return expireAfterCreate(token, until, currentTime);
        }

        @Override
        public long expireAfterRead(final String token, final Instant until, final long currentTime,
                                    final long currentDuration) {
            return currentDuration;
        }
    }
}
