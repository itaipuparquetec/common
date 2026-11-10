package br.org.itaipuparquetec.common.infrastructure.blacklist;

import com.github.benmanes.caffeine.cache.Ticker;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private final MovingTime time = new MovingTime(NOW);
    private final TokenBlacklist blacklist = new TokenBlacklist(time, time);

    @Test
    void anUnknownTokenIsNotRevoked() {
        assertThat(new TokenBlacklist().isRevoked("no-such-token")).isFalse();
    }

    @Test
    void keepsARevokedTokenUntilItExpires() throws Exception {
        final var token = tokenExpiringIn(Duration.ofMinutes(50));

        blacklist.revoke(token);
        time.advance(Duration.ofMinutes(40));

        assertThat(blacklist.isRevoked(token)).isTrue();

        time.advance(Duration.ofMinutes(10).plusSeconds(1));
        assertThat(blacklist.isRevoked(token)).isFalse();
    }

    @Test
    void doesNotKeepATokenThatAlreadyExpired() throws Exception {
        final var token = tokenExpiringIn(Duration.ofMinutes(-1));

        blacklist.revoke(token);

        assertThat(blacklist.isRevoked(token)).isFalse();
    }

    @Test
    void keepsATokenWithoutExpirationOrNotAJwtForTheLongestLifeOfAnAccessToken() throws Exception {
        final var withoutExpiration = tokenExpiringAt(null);

        blacklist.revoke(withoutExpiration);
        blacklist.revoke("opaque-refresh-token");
        time.advance(TokenBlacklist.LONGEST_ACCESS_TOKEN_LIFE.minusSeconds(1));

        assertThat(blacklist.isRevoked(withoutExpiration)).isTrue();
        assertThat(blacklist.isRevoked("opaque-refresh-token")).isTrue();

        time.advance(Duration.ofSeconds(2));
        assertThat(blacklist.isRevoked(withoutExpiration)).isFalse();
        assertThat(blacklist.isRevoked("opaque-refresh-token")).isFalse();
    }

    @Test
    void neverKeepsATokenLongerThanTheLongestLifeOfAnAccessToken() throws Exception {
        final var token = tokenExpiringIn(Duration.ofDays(30));

        blacklist.revoke(token);
        time.advance(TokenBlacklist.LONGEST_ACCESS_TOKEN_LIFE.plusSeconds(1));

        assertThat(blacklist.isRevoked(token)).isFalse();
    }

    @Test
    void revokingTheSameTokenAgainKeepsItUntilItExpires() throws Exception {
        final var token = tokenExpiringIn(Duration.ofMinutes(50));

        blacklist.revoke(token);
        time.advance(Duration.ofMinutes(20));
        blacklist.revoke(token);
        time.advance(Duration.ofMinutes(25));

        assertThat(blacklist.isRevoked(token)).isTrue();
    }

    private String tokenExpiringIn(final Duration duration) throws Exception {
        return tokenExpiringAt(Date.from(NOW.plus(duration)));
    }

    private static String tokenExpiringAt(final Date expiration) throws Exception {
        final var claims = new JWTClaimsSet.Builder().subject("maria").expirationTime(expiration).build();
        final var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner("a-secret-of-at-least-thirty-two-bytes!!"));
        return jwt.serialize();
    }

    /**
     * The clock and the ticker of the blacklist, moved together by the test.
     */
    private static final class MovingTime extends Clock implements Ticker {

        private Instant now;
        private long nanos;

        MovingTime(final Instant now) {
            this.now = now;
        }

        void advance(final Duration duration) {
            now = now.plus(duration);
            nanos += duration.toNanos();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public long read() {
            return nanos;
        }
    }
}
