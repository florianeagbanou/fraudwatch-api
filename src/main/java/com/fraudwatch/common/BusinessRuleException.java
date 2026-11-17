package com.fraudwatch.common;

/** 422: the request is well formed but breaks a business rule (e.g. insufficient funds). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
