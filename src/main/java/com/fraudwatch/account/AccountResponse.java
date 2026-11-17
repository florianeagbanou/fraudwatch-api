package com.fraudwatch.account;

import java.math.BigDecimal;
import java.time.Instant;

/** What the API returns. Entities are never exposed directly (no accidental leak of the owner's password hash). */
public record AccountResponse(Long id, String number, String owner, BigDecimal balance, String currency,
                              String homeCountry, Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getNumber(), account.getOwner().getUsername(),
                account.getBalance(), account.getCurrency(), account.getHomeCountry(), account.getCreatedAt());
    }
}
