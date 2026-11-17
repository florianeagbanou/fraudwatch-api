package com.fraudwatch.alert;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Fraud alerts", description = "Review queue for fraud analysts (ANALYST or ADMIN)")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "List alerts, optionally filtered by status (OPEN, CONFIRMED_FRAUD, FALSE_POSITIVE)")
    public List<AlertResponse> list(@RequestParam(required = false) AlertStatus status) {
        return alertService.list(status);
    }

    @PatchMapping("/{alertId}")
    @Operation(summary = "Close an alert as CONFIRMED_FRAUD or FALSE_POSITIVE")
    public AlertResponse review(Authentication auth, @PathVariable Long alertId,
                                @Valid @RequestBody ReviewAlertRequest request) {
        return alertService.review(alertId, request, auth.getName());
    }
}
