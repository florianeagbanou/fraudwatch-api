package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.FraudRule;
import com.fraudwatch.fraud.FraudSettings;
import com.fraudwatch.fraud.PastTransaction;
import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.fraud.TransactionContext;
import java.time.Duration;
import java.util.Optional;

/**
 * Two transactions in two different countries, too close in time for the person to have travelled.
 * Strong signal that the card or the credentials are used by someone else.
 */
public class ImpossibleTravelRule implements FraudRule {

    public static final String CODE = "IMPOSSIBLE_TRAVEL";
    static final int POINTS = 45;

    private final FraudSettings settings;

    public ImpossibleTravelRule(FraudSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<RuleHit> evaluate(TransactionContext context) {
        if (context.history().isEmpty()) {
            return Optional.empty();
        }
        // History is ordered most recent first.
        PastTransaction previous = context.history().get(0);
        Duration gap = Duration.between(previous.timestamp(), context.timestamp());
        boolean otherCountry = !previous.country().equalsIgnoreCase(context.country());

        if (otherCountry && gap.compareTo(settings.impossibleTravelWindow()) < 0) {
            return Optional.of(new RuleHit(CODE, POINTS,
                    previous.country() + " then " + context.country() + " within " + gap.toMinutes() + " minutes"));
        }
        return Optional.empty();
    }
}
