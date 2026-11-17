package com.fraudwatch.transaction;

import com.fraudwatch.fraud.RuleHit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionResponse(Long id, Long accountId, TransactionType type, BigDecimal amount, String merchant,
                                  String country, TransactionStatus status, int riskScore, List<RuleHit> triggeredRules,
                                  BigDecimal balanceAfter, Instant createdAt) {

    /** For history lists: the balance at that time is not stored, so it is left empty. */
    public static TransactionResponse from(BankTransaction tx) {
        return build(tx, null);
    }

    /** Right after a transaction is created: includes the account balance after it. */
    public static TransactionResponse created(BankTransaction tx) {
        return build(tx, tx.getAccount().getBalance());
    }

    private static TransactionResponse build(BankTransaction tx, BigDecimal balance) {
        return new TransactionResponse(tx.getId(), tx.getAccount().getId(), tx.getType(), tx.getAmount(),
                tx.getMerchant(), tx.getCountry(), tx.getStatus(), tx.getRiskScore(), tx.getRuleHits(),
                balance, tx.getCreatedAt());
    }
}
