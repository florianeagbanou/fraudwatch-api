package com.fraudwatch.transaction;

import com.fraudwatch.account.Account;
import com.fraudwatch.fraud.RuleHit;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Named BankTransaction to avoid confusion with Spring's @Transactional and database transactions. */
@Entity
@Table(name = "bank_transaction")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 100)
    private String merchant;

    @Column(nullable = false, length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "transaction_rule_hit", joinColumns = @JoinColumn(name = "transaction_id"))
    private List<RuleHitEmbeddable> ruleHits = new ArrayList<>();

    protected BankTransaction() {
    }

    public BankTransaction(Account account, TransactionType type, BigDecimal amount, String merchant, String country,
                           TransactionStatus status, int riskScore, List<RuleHit> hits, Instant createdAt) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.merchant = merchant;
        this.country = country;
        this.status = status;
        this.riskScore = riskScore;
        this.createdAt = createdAt;
        hits.forEach(hit -> this.ruleHits.add(new RuleHitEmbeddable(hit)));
    }

    /** Applies (or cancels) the effect of this transaction on the account balance. */
    public void applyTo(Account target) {
        if (type.isDebit()) {
            target.debit(amount);
        } else {
            target.credit(amount);
        }
    }

    public void reverseOn(Account target) {
        if (type.isDebit()) {
            target.credit(amount);
        } else {
            target.debit(amount);
        }
    }

    public void changeStatus(TransactionStatus newStatus) {
        this.status = newStatus;
    }

    public List<RuleHit> getRuleHits() {
        return ruleHits.stream().map(RuleHitEmbeddable::toRuleHit).toList();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getCountry() {
        return country;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
