package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.FraudRule;
import com.fraudwatch.fraud.FraudSettings;
import com.fraudwatch.fraud.PastTransaction;
import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.fraud.TransactionContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Compares the amount with the account's own habits.
 * 300 $ is normal for one customer and very unusual for another, so a fixed threshold is not enough.
 */
public class UnusualAmountRule implements FraudRule {

    public static final String CODE = "UNUSUAL_AMOUNT";
    static final int POINTS = 30;

    private final FraudSettings settings;

    public UnusualAmountRule(FraudSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<RuleHit> evaluate(TransactionContext context) {
        // Blocked transactions never happened, so they do not count as "habits".
        List<BigDecimal> amounts = context.history().stream()
                .filter(tx -> !tx.blocked())
                .map(PastTransaction::amount)
                .toList();

        // Without enough history we cannot say what is "normal" for this customer.
        if (amounts.size() < settings.unusualAmountMinHistory()) {
            return Optional.empty();
        }

        BigDecimal average = amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
        BigDecimal limit = average.multiply(BigDecimal.valueOf(settings.unusualAmountMultiplier()));

        if (context.amount().compareTo(limit) > 0) {
            return Optional.of(new RuleHit(CODE, POINTS,
                    "Amount is more than " + settings.unusualAmountMultiplier()
                            + "x the account average of " + average.toPlainString()));
        }
        return Optional.empty();
    }
}
