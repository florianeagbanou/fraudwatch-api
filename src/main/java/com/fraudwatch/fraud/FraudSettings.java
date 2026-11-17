package com.fraudwatch.fraud;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;

/**
 * All the thresholds used by the rules, in one place.
 * In the application they come from application.yml (see FraudConfig), so a risk team
 * could tune them without touching the code.
 */
public record FraudSettings(
        BigDecimal largeAmountThreshold,
        int unusualAmountMultiplier,
        int unusualAmountMinHistory,
        int velocityMaxTransactions,
        Duration velocityWindow,
        Duration impossibleTravelWindow,
        int nightStartHour,
        int nightEndHour,
        ZoneId zone,
        int flagThreshold,
        int blockThreshold) {

    /** Sensible defaults, also used by the unit tests. */
    public static FraudSettings defaults() {
        return new FraudSettings(
                new BigDecimal("10000"),
                5,
                3,
                5,
                Duration.ofMinutes(10),
                Duration.ofHours(2),
                0,
                5,
                ZoneId.of("America/Toronto"),
                40,
                70);
    }
}
