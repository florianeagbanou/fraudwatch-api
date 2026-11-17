package com.fraudwatch.alert;

import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.transaction.BankTransaction;
import com.fraudwatch.transaction.TransactionStatus;
import com.fraudwatch.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AlertResponse(Long id, AlertStatus status, Long transactionId, String accountNumber,
                            TransactionType type, BigDecimal amount, String country, String merchant,
                            TransactionStatus transactionStatus, int riskScore, List<RuleHit> triggeredRules,
                            Instant createdAt, String reviewedBy, Instant reviewedAt, String resolutionNote) {

    public static AlertResponse from(FraudAlert alert) {
        BankTransaction tx = alert.getTransaction();
        return new AlertResponse(alert.getId(), alert.getStatus(), tx.getId(), tx.getAccount().getNumber(),
                tx.getType(), tx.getAmount(), tx.getCountry(), tx.getMerchant(), tx.getStatus(), tx.getRiskScore(),
                tx.getRuleHits(), alert.getCreatedAt(), alert.getReviewedBy(), alert.getReviewedAt(),
                alert.getResolutionNote());
    }
}
