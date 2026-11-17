package com.fraudwatch.fraud;

import java.util.List;
import java.util.Optional;

/**
 * Runs every rule on a transaction, adds up the points and turns the score into a decision.
 *
 * <pre>
 *   score  0-39  -> APPROVED
 *   score 40-69  -> FLAGGED   (default thresholds)
 *   score 70-100 -> BLOCKED
 * </pre>
 */
public class FraudEngine {

    private static final int MAX_SCORE = 100;

    private final List<FraudRule> rules;
    private final FraudSettings settings;

    public FraudEngine(List<FraudRule> rules, FraudSettings settings) {
        this.rules = List.copyOf(rules);
        this.settings = settings;
    }

    public FraudAssessment assess(TransactionContext context) {
        List<RuleHit> hits = rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(Optional::stream)
                .toList();

        int score = Math.min(MAX_SCORE, hits.stream().mapToInt(RuleHit::points).sum());
        return new FraudAssessment(score, decide(score), hits);
    }

    private Decision decide(int score) {
        if (score >= settings.blockThreshold()) {
            return Decision.BLOCKED;
        }
        if (score >= settings.flagThreshold()) {
            return Decision.FLAGGED;
        }
        return Decision.APPROVED;
    }
}
