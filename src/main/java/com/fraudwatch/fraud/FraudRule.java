package com.fraudwatch.fraud;

import java.util.Optional;

/**
 * A single fraud detection rule.
 * Each rule looks at one risk signal and either matches (returns a hit) or not (returns empty).
 * Adding a new rule means writing one new class: the engine does not change (open/closed principle).
 */
public interface FraudRule {

    Optional<RuleHit> evaluate(TransactionContext context);
}
