package com.fraudwatch.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds "fraudwatch.jwt.*": signing secret and token lifetime. */
@ConfigurationProperties(prefix = "fraudwatch.jwt")
public record JwtProperties(String secret, Duration ttl) {

    public JwtProperties {
        // HS256 needs a key of at least 256 bits (32 bytes). Fail fast at startup otherwise.
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("fraudwatch.jwt.secret must be at least 32 bytes long");
        }
    }
}
