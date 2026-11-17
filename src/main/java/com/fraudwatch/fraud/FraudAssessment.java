package com.fraudwatch.fraud;

import java.util.List;

/** Final verdict of the engine for one transaction. */
public record FraudAssessment(int riskScore, Decision decision, List<RuleHit> hits) {

    public FraudAssessment {
        hits = List.copyOf(hits);
    }
}
