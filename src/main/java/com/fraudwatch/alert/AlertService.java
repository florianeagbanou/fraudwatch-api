package com.fraudwatch.alert;

import com.fraudwatch.account.Account;
import com.fraudwatch.audit.AuditAction;
import com.fraudwatch.audit.AuditService;
import com.fraudwatch.common.NotFoundException;
import com.fraudwatch.transaction.BankTransaction;
import com.fraudwatch.transaction.TransactionStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alerts;
    private final AuditService audit;
    private final Clock clock;

    public AlertService(AlertRepository alerts, AuditService audit, Clock clock) {
        this.alerts = alerts;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void open(BankTransaction transaction) {
        alerts.save(new FraudAlert(transaction, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> list(AlertStatus status) {
        List<FraudAlert> found = status == null
                ? alerts.findAllByOrderByCreatedAtDescIdDesc()
                : alerts.findByStatusOrderByCreatedAtDescIdDesc(status);
        return found.stream().map(AlertResponse::from).toList();
    }

    /**
     * An analyst closes an alert. The money follows the decision:
     *
     * <pre>
     *                    | transaction FLAGGED (money moved) | transaction BLOCKED (money did not move)
     *   FALSE_POSITIVE   | stays, becomes APPROVED           | money moves now, becomes APPROVED
     *   CONFIRMED_FRAUD  | money is reversed, becomes BLOCKED| stays BLOCKED
     * </pre>
     *
     * Everything happens in one database transaction: if the money cannot move (insufficient funds),
     * the alert stays open.
     */
    @Transactional
    public AlertResponse review(Long alertId, ReviewAlertRequest request, String analyst) {
        FraudAlert alert = alerts.findById(alertId).orElseThrow(() -> new NotFoundException("Alert not found"));
        BankTransaction tx = alert.getTransaction();
        Account account = tx.getAccount();

        alert.close(request.resolution(), analyst, request.note(), Instant.now(clock));

        if (request.resolution() == AlertStatus.FALSE_POSITIVE) {
            if (tx.getStatus() == TransactionStatus.BLOCKED) {
                tx.applyTo(account);
            }
            tx.changeStatus(TransactionStatus.APPROVED);
        } else if (tx.getStatus() == TransactionStatus.FLAGGED) {
            tx.reverseOn(account);
            tx.changeStatus(TransactionStatus.BLOCKED);
        }

        audit.record(analyst, AuditAction.ALERT_REVIEWED,
                "Alert " + alertId + " closed as " + request.resolution() + " (transaction " + tx.getId() + ")");
        return AlertResponse.from(alert);
    }
}
