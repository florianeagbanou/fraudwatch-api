package com.fraudwatch.common;

/** 409: the request conflicts with the current state (username taken, alert already reviewed...). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
