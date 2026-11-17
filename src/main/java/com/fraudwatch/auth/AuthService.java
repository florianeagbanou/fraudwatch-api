package com.fraudwatch.auth;

import com.fraudwatch.audit.AuditAction;
import com.fraudwatch.audit.AuditService;
import com.fraudwatch.common.AccountLockedException;
import com.fraudwatch.common.ConflictException;
import com.fraudwatch.common.InvalidCredentialsException;
import com.fraudwatch.user.AppUser;
import com.fraudwatch.user.Role;
import com.fraudwatch.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttempts;
    private final AuditService audit;
    private final Clock clock;

    /**
     * When the username does not exist we still check the password against this hash,
     * so the response time does not reveal which usernames exist (user enumeration).
     */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
                       LoginAttemptService loginAttempts, AuditService audit, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttempts = loginAttempts;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** Public sign-up always creates a customer (USER). Analysts and admins are created by the bank. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken");
        }
        AppUser user = users.save(new AppUser(request.username(), passwordEncoder.encode(request.password()),
                Role.USER, Instant.now(clock)));
        audit.record(user.getUsername(), AuditAction.USER_REGISTERED, "New customer account");
        return UserResponse.from(user);
    }

    public TokenResponse login(LoginRequest request) {
        String username = request.username();

        Optional<Duration> lock = loginAttempts.lockRemaining(username);
        if (lock.isPresent()) {
            audit.record(username, AuditAction.LOGIN_LOCKED, "Login refused, username locked");
            throw new AccountLockedException(lock.get());
        }

        Optional<AppUser> user = users.findByUsername(username);
        String hash = user.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (user.isEmpty() || !passwordMatches) {
            boolean nowLocked = loginAttempts.recordFailure(username);
            audit.record(username, AuditAction.LOGIN_FAILURE, nowLocked ? "Failed login, username now locked" : "Failed login");
            throw new InvalidCredentialsException();
        }

        loginAttempts.recordSuccess(username);
        audit.record(username, AuditAction.LOGIN_SUCCESS, null);
        return tokenService.issue(user.get());
    }
}
