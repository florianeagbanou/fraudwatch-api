package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.FraudRule;
import com.fraudwatch.fraud.FraudSettings;
import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.fraud.TransactionContext;
import java.time.Instant;
import java.util.Optional;

/**
 * Detects bursts of transactions in a short time, typical of a stolen card being "tested" or emptied.
 */
public class VelocityRule implements FraudRule {

    public static final String CODE = "VELOCITY";
    static final int POINTS = 35;

    private final FraudSettings settings;

    public VelocityRule(FraudSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<RuleHit> evaluate(TransactionContext context) {
        Instant windowStart = context.timestamp().minus(settings.velocityWindow());
        long recent = context.history().stream()
                .filter(tx -> tx.timestamp().isAfter(windowStart))
                .count();

        // "recent" are the previous ones: with the current one, that makes recent + 1.
        if (recent >= settings.velocityMaxTransactions()) {
            return Optional.of(new RuleHit(CODE, POINTS,
                    (recent + 1) + " transactions within " + settings.velocityWindow().toMinutes() + " minutes"));
        }
        return Optional.empty();
    }
}
