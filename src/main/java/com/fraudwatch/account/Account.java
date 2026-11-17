package com.fraudwatch.account;

import com.fraudwatch.common.BusinessRuleException;
import com.fraudwatch.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    /** Money is always BigDecimal, never double: 0.1 + 0.2 must equal 0.3. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "home_country", nullable = false, length = 2)
    private String homeCountry;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Optimistic locking: if two requests update the same account at the same time,
     * the second one fails instead of silently overwriting the balance.
     */
    @Version
    private long version;

    protected Account() {
    }

    public Account(String number, AppUser owner, String currency, String homeCountry, Instant createdAt) {
        this.number = number;
        this.owner = owner;
        this.currency = currency;
        this.homeCountry = homeCountry;
        this.createdAt = createdAt;
        this.balance = BigDecimal.ZERO.setScale(2);
    }

    // Business methods: the balance can only change through these two methods.

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient funds");
        }
        balance = balance.subtract(amount);
    }

    public boolean isOwnedBy(String username) {
        return owner.getUsername().equals(username);
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public AppUser getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public String getHomeCountry() {
        return homeCountry;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
