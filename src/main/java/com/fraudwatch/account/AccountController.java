package com.fraudwatch.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Bank accounts of the logged-in customer")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Authentication.getName() is the "sub" claim of the JWT, i.e. the username.

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a new account")
    public AccountResponse create(Authentication auth, @Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(auth.getName(), request);
    }

    @GetMapping
    @Operation(summary = "List my accounts")
    public List<AccountResponse> list(Authentication auth) {
        return accountService.listFor(auth.getName());
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get one of my accounts")
    public AccountResponse get(Authentication auth, @PathVariable Long accountId) {
        return accountService.get(auth.getName(), accountId);
    }
}
