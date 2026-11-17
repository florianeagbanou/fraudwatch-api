package com.fraudwatch.transaction;

public enum TransactionType {
    /** Money in. */
    DEPOSIT,
    /** Cash withdrawal. */
    WITHDRAWAL,
    /** Card payment to a merchant. */
    PAYMENT;

    public boolean isDebit() {
        return this != DEPOSIT;
    }
}
