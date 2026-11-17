package com.fraudwatch.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final int MAX_DETAILS = 500;

    private final AuditRepository repository;
    private final Clock clock;

    public AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Records an event in its own database transaction (REQUIRES_NEW):
     * a failed login is still written even if the surrounding operation fails and rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, AuditAction action, String details) {
        String safeDetails = details != null && details.length() > MAX_DETAILS ? details.substring(0, MAX_DETAILS) : details;
        repository.save(new AuditEvent(Instant.now(clock), truncate(actor), action, safeDetails));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> latest(int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        return repository.findAllByOrderByIdDesc(PageRequest.of(0, size)).stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    private static String truncate(String actor) {
        // Usernames are max 30 characters, but a failed login can contain anything.
        return actor.length() > 30 ? actor.substring(0, 30) : actor;
    }
}
