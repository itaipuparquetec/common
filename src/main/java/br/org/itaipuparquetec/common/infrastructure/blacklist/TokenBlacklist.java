package br.org.itaipuparquetec.common.infrastructure.blacklist;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenBlacklist {

    private final Cache<String, Boolean> blacklist;

    public TokenBlacklist() {
        this.blacklist = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .build();
    }

    public void revoke(String token) {
        blacklist.put(token, true);
    }

    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(blacklist.getIfPresent(token));
    }
}
