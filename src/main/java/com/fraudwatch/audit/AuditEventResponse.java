package com.fraudwatch.audit;

import java.time.Instant;

public record AuditEventResponse(Long id, Instant occurredAt, String actor, AuditAction action, String details) {

    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getOccurredAt(), event.getActor(),
                event.getAction(), event.getDetails());
    }
}
