package com.fraudwatch.config;

import com.fraudwatch.fraud.FraudEngine;
import com.fraudwatch.fraud.FraudSettings;
import com.fraudwatch.fraud.rules.ForeignCountryRule;
import com.fraudwatch.fraud.rules.ImpossibleTravelRule;
import com.fraudwatch.fraud.rules.LargeAmountRule;
import com.fraudwatch.fraud.rules.NightActivityRule;
import com.fraudwatch.fraud.rules.UnusualAmountRule;
import com.fraudwatch.fraud.rules.VelocityRule;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free fraud engine into Spring.
 * The rules themselves know nothing about Spring: this class is the only bridge.
 */
@Configuration
public class FraudConfig {

    /** A single clock for the whole app, so tests can freeze time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public FraudSettings fraudSettings(FraudProperties properties) {
        return properties.toSettings();
    }

    @Bean
    public FraudEngine fraudEngine(FraudSettings settings) {
        return new FraudEngine(List.of(
                new LargeAmountRule(settings),
                new UnusualAmountRule(settings),
                new VelocityRule(settings),
                new ForeignCountryRule(),
                new ImpossibleTravelRule(settings),
                new NightActivityRule(settings)), settings);
    }
}
