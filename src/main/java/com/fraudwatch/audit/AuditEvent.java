package com.fraudwatch.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One line of the audit log. Audit events are only ever inserted, never updated or deleted. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 30)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(length = 500)
    private String details;

    protected AuditEvent() {
    }

    public AuditEvent(Instant occurredAt, String actor, AuditAction action, String details) {
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.action = action;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}
