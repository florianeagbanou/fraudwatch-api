package com.fraudwatch.config;

import com.fraudwatch.user.AppUser;
import com.fraudwatch.user.Role;
import com.fraudwatch.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates an analyst and an admin at startup so the demo works right away.
 * Disabled with fraudwatch.demo-data.enabled=false (never enable these accounts in production).
 */
@Component
@ConditionalOnProperty(prefix = "fraudwatch.demo-data", name = "enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataLoader(UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("analyst", "Analyst123!", Role.ANALYST);
        createIfMissing("admin", "Admin123!", Role.ADMIN);
    }

    private void createIfMissing(String username, String password, Role role) {
        if (!users.existsByUsername(username)) {
            users.save(new AppUser(username, passwordEncoder.encode(password), role, Instant.now(clock)));
            log.warn("Demo user created: {} ({}). Disable demo data in production.", username, role);
        }
    }
}
