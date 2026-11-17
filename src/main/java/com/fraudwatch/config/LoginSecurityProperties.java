package com.fraudwatch.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds "fraudwatch.security.*": brute-force protection on the login endpoint. */
@ConfigurationProperties(prefix = "fraudwatch.security")
public record LoginSecurityProperties(int maxFailedLogins, Duration lockoutDuration) {
}
