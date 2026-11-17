package com.fraudwatch.transaction;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotNull TransactionType type,
        @NotNull
        @DecimalMin(value = "0.01", message = "must be at least 0.01")
        @DecimalMax(value = "1000000.00", message = "must be at most 1000000.00")
        @Digits(integer = 7, fraction = 2, message = "must have at most 2 decimals")
        BigDecimal amount,
        @Size(max = 100) String merchant,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "must be a 2-letter ISO country code, e.g. CA")
        String country) {
}
