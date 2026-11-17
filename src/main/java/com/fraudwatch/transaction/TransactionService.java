package com.fraudwatch.transaction;

import com.fraudwatch.account.Account;
import com.fraudwatch.account.AccountService;
import com.fraudwatch.alert.AlertService;
import com.fraudwatch.audit.AuditAction;
import com.fraudwatch.audit.AuditService;
import com.fraudwatch.common.BusinessRuleException;
import com.fraudwatch.fraud.Decision;
import com.fraudwatch.fraud.FraudAssessment;
import com.fraudwatch.fraud.FraudEngine;
import com.fraudwatch.fraud.PastTransaction;
import com.fraudwatch.fraud.TransactionContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final AccountService accountService;
    private final AlertService alertService;
    private final FraudEngine fraudEngine;
    private final AuditService audit;
    private final Clock clock;

    public TransactionService(TransactionRepository transactions, AccountService accountService,
                              AlertService alertService, FraudEngine fraudEngine, AuditService audit, Clock clock) {
        this.transactions = transactions;
        this.accountService = accountService;
        this.alertService = alertService;
        this.fraudEngine = fraudEngine;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * The heart of the application:
     * <ol>
     *   <li>check the account belongs to the caller,</li>
     *   <li>refuse a debit larger than the balance,</li>
     *   <li>ask the fraud engine for a risk score,</li>
     *   <li>move the money unless the transaction is blocked,</li>
     *   <li>open an alert for flagged and blocked transactions.</li>
     * </ol>
     * All of it is one database transaction: either everything is saved, or nothing is.
     */
    @Transactional
    public TransactionResponse create(String username, Long accountId, CreateTransactionRequest request) {
        Account account = accountService.findOwned(username, accountId);
        Instant now = Instant.now(clock);

        if (request.type().isDebit() && account.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessRuleException("Insufficient funds");
        }

        FraudAssessment assessment = fraudEngine.assess(new TransactionContext(
                request.amount(), request.country(), account.getHomeCountry(), now, historyOf(account)));

        TransactionStatus status = toStatus(assessment.decision());
        BankTransaction tx = new BankTransaction(account, request.type(), request.amount(), request.merchant(),
                request.country(), status, assessment.riskScore(), assessment.hits(), now);

        if (status != TransactionStatus.BLOCKED) {
            tx.applyTo(account);
        }
        tx = transactions.save(tx);

        if (status != TransactionStatus.APPROVED) {
            alertService.open(tx);
        }

        audit.record(username, AuditAction.TRANSACTION_CREATED,
                request.type() + " " + request.amount() + " on " + account.getNumber() + " -> " + status
                        + " (score " + assessment.riskScore() + ")");
        return TransactionResponse.created(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(String username, Long accountId) {
        Account account = accountService.findOwned(username, accountId);
        return transactions.findByAccountIdOrderByCreatedAtDescIdDesc(account.getId()).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    private List<PastTransaction> historyOf(Account account) {
        return transactions.findTop50ByAccountIdOrderByCreatedAtDescIdDesc(account.getId()).stream()
                .map(tx -> new PastTransaction(tx.getAmount(), tx.getCountry(), tx.getCreatedAt(),
                        tx.getStatus() == TransactionStatus.BLOCKED))
                .toList();
    }

    private static TransactionStatus toStatus(Decision decision) {
        return switch (decision) {
            case APPROVED -> TransactionStatus.APPROVED;
            case FLAGGED -> TransactionStatus.FLAGGED;
            case BLOCKED -> TransactionStatus.BLOCKED;
        };
    }
}
