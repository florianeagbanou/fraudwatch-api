package com.fraudwatch.alert;

import com.fraudwatch.common.BusinessRuleException;
import com.fraudwatch.common.ConflictException;
import com.fraudwatch.transaction.BankTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "fraud_alert")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", unique = true)
    private BankTransaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_by", length = 30)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    protected FraudAlert() {
    }

    public FraudAlert(BankTransaction transaction, Instant createdAt) {
        this.transaction = transaction;
        this.status = AlertStatus.OPEN;
        this.createdAt = createdAt;
    }

    /** An alert can be closed only once: reviewing it twice would apply the money movement twice. */
    public void close(AlertStatus resolution, String analyst, String note, Instant when) {
        if (status != AlertStatus.OPEN) {
            throw new ConflictException("Alert already reviewed");
        }
        if (resolution == AlertStatus.OPEN) {
            throw new BusinessRuleException("Resolution must be CONFIRMED_FRAUD or FALSE_POSITIVE");
        }
        this.status = resolution;
        this.reviewedBy = analyst;
        this.reviewedAt = when;
        this.resolutionNote = note;
    }

    public Long getId() {
        return id;
    }

    public BankTransaction getTransaction() {
        return transaction;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }
}
