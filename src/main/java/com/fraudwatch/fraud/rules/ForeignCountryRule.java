package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.FraudRule;
import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.fraud.TransactionContext;
import java.util.Optional;

/** A transaction outside the customer's home country is a weak signal on its own, so it adds few points. */
public class ForeignCountryRule implements FraudRule {

    public static final String CODE = "FOREIGN_COUNTRY";
    static final int POINTS = 15;

    @Override
    public Optional<RuleHit> evaluate(TransactionContext context) {
        if (!context.country().equalsIgnoreCase(context.homeCountry())) {
            return Optional.of(new RuleHit(CODE, POINTS,
                    "Transaction in " + context.country() + ", home country is " + context.homeCountry()));
        }
        return Optional.empty();
    }
}
