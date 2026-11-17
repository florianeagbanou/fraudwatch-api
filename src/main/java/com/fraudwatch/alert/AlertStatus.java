package com.fraudwatch.alert;

public enum AlertStatus {
    /** Waiting for an analyst. */
    OPEN,
    /** The analyst confirmed it was fraud. */
    CONFIRMED_FRAUD,
    /** The analyst decided it was a legitimate transaction. */
    FALSE_POSITIVE
}
