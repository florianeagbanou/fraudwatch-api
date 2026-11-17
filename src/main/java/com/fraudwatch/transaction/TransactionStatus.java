package com.fraudwatch.transaction;

public enum TransactionStatus {
    /** Money moved, nothing suspicious (or cleared by an analyst). */
    APPROVED,
    /** Money moved, but an analyst must review it. */
    FLAGGED,
    /** Money did not move (or was reversed after a confirmed fraud). */
    BLOCKED
}
