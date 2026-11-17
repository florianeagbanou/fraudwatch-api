package com.fraudwatch.common;

/** 404. Also used when a resource exists but belongs to someone else, so we never reveal it exists. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
