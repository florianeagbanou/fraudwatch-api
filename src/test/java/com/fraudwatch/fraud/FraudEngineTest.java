package com.fraudwatch.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FraudEngineTest {

    private static final FraudSettings SETTINGS = FraudSettings.defaults();
    private static final TransactionContext ANY = new TransactionContext(
            BigDecimal.TEN, "CA", "CA", Instant.parse("2026-01-15T17:00:00Z"), List.of());

    /** A fake rule that always matches with the given points. */
    private static FraudRule rule(String code, int points) {
        return context -> Optional.of(new RuleHit(code, points, "test"));
    }

    @Test
    void approvesWhenNoRuleMatches() {
        FraudEngine engine = new FraudEngine(List.of(context -> Optional.empty()), SETTINGS);

        FraudAssessment result = engine.assess(ANY);

        assertThat(result.decision()).isEqualTo(Decision.APPROVED);
        assertThat(result.riskScore()).isZero();
        assertThat(result.hits()).isEmpty();
    }

    @Test
    void addsUpPointsFromEveryMatchingRule() {
        FraudEngine engine = new FraudEngine(List.of(rule("A", 15), rule("B", 10)), SETTINGS);

        FraudAssessment result = engine.assess(ANY);

        assertThat(result.riskScore()).isEqualTo(25);
        assertThat(result.hits()).extracting(RuleHit::code).containsExactly("A", "B");
    }

    @Test
    void flagsAtTheFlagThreshold() {
        FraudEngine engine = new FraudEngine(List.of(rule("A", 40)), SETTINGS);

        assertThat(engine.assess(ANY).decision()).isEqualTo(Decision.FLAGGED);
    }

    @Test
    void justBelowFlagThresholdIsApproved() {
        FraudEngine engine = new FraudEngine(List.of(rule("A", 39)), SETTINGS);

        assertThat(engine.assess(ANY).decision()).isEqualTo(Decision.APPROVED);
    }

    @Test
    void blocksAtTheBlockThreshold() {
        FraudEngine engine = new FraudEngine(List.of(rule("A", 40), rule("B", 30)), SETTINGS);

        assertThat(engine.assess(ANY).decision()).isEqualTo(Decision.BLOCKED);
    }

    @Test
    void capsTheScoreAtOneHundred() {
        FraudEngine engine = new FraudEngine(List.of(rule("A", 80), rule("B", 80)), SETTINGS);

        assertThat(engine.assess(ANY).riskScore()).isEqualTo(100);
    }
}
