package com.fraudwatch.fraud;

/**
 * Result of a rule that matched: which rule, how many risk points it adds, and a readable reason.
 */
public record RuleHit(String code, int points, String reason) {
}
