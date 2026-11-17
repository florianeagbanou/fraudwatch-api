package com.fraudwatch.transaction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
@Tag(name = "Transactions", description = "Transactions on one of my accounts, checked for fraud in real time")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Make a transaction. The response says if it was APPROVED, FLAGGED or BLOCKED, and why.")
    public TransactionResponse create(Authentication auth, @PathVariable Long accountId,
                                      @Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.create(auth.getName(), accountId, request);
    }

    @GetMapping
    @Operation(summary = "Transaction history, newest first")
    public List<TransactionResponse> list(Authentication auth, @PathVariable Long accountId) {
        return transactionService.list(auth.getName(), accountId);
    }
}
