package com.fraudwatch.fraud;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A previous transaction, reduced to the fields the rules use.
 *
 * @param blocked true if the transaction was blocked (it never moved money)
 */
public record PastTransaction(BigDecimal amount, String country, Instant timestamp, boolean blocked) {
}
