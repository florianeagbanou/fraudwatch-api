package com.fraudwatch.fraud;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Everything a fraud rule needs to know about a transaction, without any database or Spring dependency.
 * Keeping the rules independent from the framework makes them fast and easy to unit test.
 *
 * @param amount      amount of the new transaction
 * @param country     ISO country code where the transaction happens (e.g. "CA")
 * @param homeCountry ISO country code of the account holder
 * @param timestamp   when the transaction happens
 * @param history     previous transactions of the same account, most recent first
 */
public record TransactionContext(
        BigDecimal amount,
        String country,
        String homeCountry,
        Instant timestamp,
        List<PastTransaction> history) {

    public TransactionContext {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
