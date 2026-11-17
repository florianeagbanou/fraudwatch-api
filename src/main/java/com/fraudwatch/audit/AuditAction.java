package com.fraudwatch.audit;

/** Security-relevant events we keep a trace of. */
public enum AuditAction {
    USER_REGISTERED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGIN_LOCKED,
    ACCOUNT_CREATED,
    TRANSACTION_CREATED,
    ALERT_REVIEWED
}
