package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.FraudRule;
import com.fraudwatch.fraud.FraudSettings;
import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.fraud.TransactionContext;
import java.util.Optional;

/** Transactions in the middle of the night (bank's local time) are slightly more suspicious. */
public class NightActivityRule implements FraudRule {

    public static final String CODE = "NIGHT_ACTIVITY";
    static final int POINTS = 10;

    private final FraudSettings settings;

    public NightActivityRule(FraudSettings settings) {
        this.settings = settings;
    }

    @Override
    public Optional<RuleHit> evaluate(TransactionContext context) {
        int hour = context.timestamp().atZone(settings.zone()).getHour();
        if (hour >= settings.nightStartHour() && hour < settings.nightEndHour()) {
            return Optional.of(new RuleHit(CODE, POINTS, "Transaction at " + hour + "h local time"));
        }
        return Optional.empty();
    }
}
