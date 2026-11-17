package com.fraudwatch.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fraudwatch.account.Account;
import com.fraudwatch.account.AccountService;
import com.fraudwatch.alert.AlertService;
import com.fraudwatch.audit.AuditService;
import com.fraudwatch.common.BusinessRuleException;
import com.fraudwatch.fraud.Decision;
import com.fraudwatch.fraud.FraudAssessment;
import com.fraudwatch.fraud.FraudEngine;
import com.fraudwatch.fraud.RuleHit;
import com.fraudwatch.user.AppUser;
import com.fraudwatch.user.Role;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the service alone: the database, the fraud engine and the alerts are replaced by mocks.
 * We check the service's own decisions: does the money move? Is an alert opened?
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactions;
    @Mock
    private AccountService accountService;
    @Mock
    private AlertService alertService;
    @Mock
    private FraudEngine fraudEngine;
    @Mock
    private AuditService audit;

    private TransactionService service;
    private Account account;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T17:00:00Z"), ZoneOffset.UTC);
        service = new TransactionService(transactions, accountService, alertService, fraudEngine, audit, clock);

        AppUser owner = new AppUser("alice", "hash", Role.USER, Instant.now());
        account = new Account("FW-0000000001", owner, "CAD", "CA", Instant.now());
        account.credit(new BigDecimal("1000.00"));

        when(accountService.findOwned("alice", 1L)).thenReturn(account);
        // save() returns what it receives, like the real repository would
        // lenient: some tests stop before saving, Mockito must not complain the stub is unused
        lenient().when(transactions.save(any(BankTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void engineSays(Decision decision, int score) {
        List<RuleHit> hits = score == 0 ? List.of() : List.of(new RuleHit("TEST", score, "test"));
        when(fraudEngine.assess(any())).thenReturn(new FraudAssessment(score, decision, hits));
        when(transactions.findTop50ByAccountIdOrderByCreatedAtDescIdDesc(any())).thenReturn(List.of());
    }

    private static CreateTransactionRequest payment(String amount) {
        return new CreateTransactionRequest(TransactionType.PAYMENT, new BigDecimal(amount), "Shop", "CA");
    }

    @Test
    void approvedPaymentMovesMoneyWithoutAlert() {
        engineSays(Decision.APPROVED, 0);

        TransactionResponse result = service.create("alice", 1L, payment("200.00"));

        assertThat(result.status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        verify(alertService, never()).open(any());
    }

    @Test
    void flaggedPaymentMovesMoneyAndOpensAlert() {
        engineSays(Decision.FLAGGED, 45);

        TransactionResponse result = service.create("alice", 1L, payment("200.00"));

        assertThat(result.status()).isEqualTo(TransactionStatus.FLAGGED);
        assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        verify(alertService).open(any(BankTransaction.class));
    }

    @Test
    void blockedPaymentKeepsMoneyAndOpensAlert() {
        engineSays(Decision.BLOCKED, 90);

        TransactionResponse result = service.create("alice", 1L, payment("200.00"));

        assertThat(result.status()).isEqualTo(TransactionStatus.BLOCKED);
        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
        verify(alertService).open(any(BankTransaction.class));
    }

    @Test
    void depositIncreasesBalance() {
        engineSays(Decision.APPROVED, 0);

        service.create("alice", 1L, new CreateTransactionRequest(TransactionType.DEPOSIT, new BigDecimal("50.00"), null, "CA"));

        assertThat(account.getBalance()).isEqualByComparingTo("1050.00");
    }

    @Test
    void refusesDebitLargerThanBalanceBeforeCallingTheEngine() {
        assertThatThrownBy(() -> service.create("alice", 1L, payment("1000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Insufficient funds");

        verify(fraudEngine, never()).assess(any());
        verify(transactions, never()).findTop50ByAccountIdOrderByCreatedAtDescIdDesc(any());
    }
}
