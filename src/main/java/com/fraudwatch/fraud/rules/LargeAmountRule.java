package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.FraudRule;
import com.fraudwatch.fraud.FraudSettings;
import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.fraud.TransactionContext;
import java.util.Optional;

/** Flags any transaction at or above a fixed amount (large transfers deserve a second look). */
public class LargeAmountRule implements FraudRule {

    public static final String CODE = "LARGE_AMOUNT";
    static final int POINTS = 40;

    private final FraudSettings settings;

    public LargeAmountRule(FraudSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<RuleHit> evaluate(TransactionContext context) {
        if (context.amount().compareTo(settings.largeAmountThreshold()) >= 0) {
            return Optional.of(new RuleHit(CODE, POINTS,
                    "Amount " + context.amount().toPlainString()
                            + " is at or above the " + settings.largeAmountThreshold().toPlainString() + " threshold"));
        }
        return Optional.empty();
    }
}
