package br.com.insurehub.audit;

import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository repository;
    private final Clock clock;

    public AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Participa da transação de quem chamou: se a operação for desfeita, o registro também é. */
    public void record(Long userId, AuditAction action, String entity, Object entityId, String details) {
        repository.save(new AuditLog(userId, action.name(), entity,
                entityId == null ? null : String.valueOf(entityId), details, clock.instant()));
    }
}
