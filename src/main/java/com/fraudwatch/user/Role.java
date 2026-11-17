package com.fraudwatch.user;

public enum Role {
    /** A bank customer: owns accounts and makes transactions. */
    USER,
    /** A fraud analyst: reviews alerts. */
    ANALYST,
    /** An administrator: can also read the audit log. */
    ADMIN
}
