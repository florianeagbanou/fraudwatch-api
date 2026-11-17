package com.fraudwatch.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "may only contain letters, digits, dots, dashes and underscores")
        String username,

        @NotBlank
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain at least one letter and one digit")
        String password) {

    /** Never print the password, even in logs or debugger output. */
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", password=***]";
    }
}
