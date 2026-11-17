package com.fraudwatch.transaction;

import com.fraudwatch.fraud.RuleHit;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Database version of a {@link RuleHit}, stored in the transaction_rule_hit table. */
@Embeddable
public class RuleHitEmbeddable {

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false, length = 255)
    private String reason;

    protected RuleHitEmbeddable() {
    }

    public RuleHitEmbeddable(RuleHit hit) {
        this.code = hit.code();
        this.points = hit.points();
        this.reason = hit.reason().length() > 255 ? hit.reason().substring(0, 255) : hit.reason();
    }

    public RuleHit toRuleHit() {
        return new RuleHit(code, points, reason);
    }
}
