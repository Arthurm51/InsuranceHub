package br.com.insurehub.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit")
@Tag(name = "Admin - Auditoria")
public class AdminAuditController {

    private final AuditRepository repository;

    public AdminAuditController(AuditRepository repository) {
        this.repository = repository;
    }

    public record AuditEntry(Long id, Long userId, String action, String entity, String entityId, String details,
                             Instant createdAt) {}

    @GetMapping
    @Operation(summary = "Consulta o log de auditoria (RN-10)")
    public Page<AuditEntry> search(@RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) String action,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        return repository.search(userId, action, PageRequest.of(Math.max(page, 0), safeSize))
                .map(a -> new AuditEntry(a.getId(), a.getUserId(), a.getAction(), a.getEntity(), a.getEntityId(),
                        a.getDetails(), a.getCreatedAt()));
    }
}
