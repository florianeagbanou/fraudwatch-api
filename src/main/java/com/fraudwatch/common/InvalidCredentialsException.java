package com.fraudwatch.common;

/** 401: wrong username or password. The message never says which one was wrong. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
