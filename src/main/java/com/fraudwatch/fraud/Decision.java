package com.fraudwatch.fraud;

/** What happens to a transaction after the fraud check. */
public enum Decision {
    /** Low risk: the money moves normally. */
    APPROVED,
    /** Medium risk: the money moves, but an analyst must review it. */
    FLAGGED,
    /** High risk: the money does not move until an analyst releases it. */
    BLOCKED
}
