package com.fraudwatch.alert;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewAlertRequest(@NotNull AlertStatus resolution, @Size(max = 500) String note) {
}
