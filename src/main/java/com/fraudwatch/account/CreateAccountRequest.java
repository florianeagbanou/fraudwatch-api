package com.fraudwatch.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "must be a 2-letter ISO country code, e.g. CA")
        String homeCountry,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code, e.g. CAD")
        String currency) {
}
