package com.fraudwatch.config;

import com.fraudwatch.fraud.FraudSettings;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the "fraudwatch.rules.*" section of application.yml. */
@ConfigurationProperties(prefix = "fraudwatch.rules")
public record FraudProperties(
        BigDecimal largeAmountThreshold,
        int unusualAmountMultiplier,
        int unusualAmountMinHistory,
        int velocityMaxTransactions,
        Duration velocityWindow,
        Duration impossibleTravelWindow,
        int nightStartHour,
        int nightEndHour,
        String zone,
        int flagThreshold,
        int blockThreshold) {

    public FraudSettings toSettings() {
        return new FraudSettings(
                largeAmountThreshold,
                unusualAmountMultiplier,
                unusualAmountMinHistory,
                velocityMaxTransactions,
                velocityWindow,
                impossibleTravelWindow,
                nightStartHour,
                nightEndHour,
                ZoneId.of(zone),
                flagThreshold,
                blockThreshold);
    }
}
