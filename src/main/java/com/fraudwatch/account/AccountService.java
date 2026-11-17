package com.fraudwatch.account;

import com.fraudwatch.audit.AuditAction;
import com.fraudwatch.audit.AuditService;
import com.fraudwatch.common.NotFoundException;
import com.fraudwatch.user.AppUser;
import com.fraudwatch.user.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final UserRepository users;
    private final AuditService audit;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accounts, UserRepository users, AuditService audit, Clock clock) {
        this.accounts = accounts;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse create(String username, CreateAccountRequest request) {
        AppUser owner = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Account account = accounts.save(new Account(newAccountNumber(), owner, request.currency(),
                request.homeCountry(), Instant.now(clock)));
        audit.record(username, AuditAction.ACCOUNT_CREATED, "Account " + account.getNumber());
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listFor(String username) {
        return accounts.findByOwnerUsernameOrderByIdAsc(username).stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String username, Long accountId) {
        return AccountResponse.from(findOwned(username, accountId));
    }

    /**
     * Loads an account only if it belongs to the caller.
     * Someone else's account gives the same 404 as a missing one: this prevents IDOR attacks
     * (guessing other ids) and does not even confirm that the id exists.
     */
    @Transactional(readOnly = true)
    public Account findOwned(String username, Long accountId) {
        return accounts.findById(accountId)
                .filter(account -> account.isOwnedBy(username))
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    /** Account numbers like FW-4829105736, random so they cannot be guessed in sequence. */
    private String newAccountNumber() {
        String number;
        do {
            number = "FW-" + String.format("%010d", random.nextLong(10_000_000_000L));
        } while (accounts.existsByNumber(number));
        return number;
    }
}
