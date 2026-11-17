package com.fraudwatch.auth;

import com.fraudwatch.config.LoginSecurityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection: after N failed logins in a row, the username is locked for a while.
 *
 * <p>Kept in memory on purpose to stay simple. With several API instances behind a load balancer,
 * this state would move to a shared store such as Redis.
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final LoginSecurityProperties properties;
    private final Clock clock;

    public LoginAttemptService(LoginSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Remaining lock time, if the username is currently locked. */
    public Optional<Duration> lockRemaining(String username) {
        Attempts current = attempts.get(key(username));
        if (current == null || current.lockedUntil() == null) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        if (now.isBefore(current.lockedUntil())) {
            return Optional.of(Duration.between(now, current.lockedUntil()));
        }
        attempts.remove(key(username));
        return Optional.empty();
    }

    /** Records a failure. Returns true if this failure triggered a lock. */
    public boolean recordFailure(String username) {
        Attempts updated = attempts.merge(key(username), new Attempts(1, null),
                (old, ignored) -> new Attempts(old.failures() + 1, null));
        if (updated.failures() >= properties.maxFailedLogins()) {
            attempts.put(key(username), new Attempts(updated.failures(),
                    Instant.now(clock).plus(properties.lockoutDuration())));
            return true;
        }
        return false;
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username.toLowerCase();
    }
}
