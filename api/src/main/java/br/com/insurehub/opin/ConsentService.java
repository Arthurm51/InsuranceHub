package br.com.insurehub.opin;

import br.com.insurehub.audit.AuditAction;
import br.com.insurehub.audit.AuditService;
import br.com.insurehub.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {

    public static final int VALIDITY_MONTHS = 12;

    private final ConsentRepository consents;
    private final ExternalPolicyRepository cache;
    private final AuditService audit;
    private final Clock clock;

    public ConsentService(ConsentRepository consents, ExternalPolicyRepository cache, AuditService audit,
                          Clock clock) {
        this.consents = consents;
        this.cache = cache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Consent grant(Long customerId) {
        if (findValid(customerId).isPresent()) {
            throw ApiException.conflict("CONSENT_ALREADY_ACTIVE", "Já existe consentimento ativo");
        }
        Instant now = clock.instant();
        Instant expiresAt = ZonedDateTime.now(clock).plusMonths(VALIDITY_MONTHS).toInstant();
        Consent consent = consents.saveAndFlush(new Consent(customerId, now, expiresAt));
        audit.record(customerId, AuditAction.CONSENT_GRANTED, "CONSENT", consent.getId(), "até " + expiresAt);
        return consent;
    }

    /** Consentimento ACTIVE e dentro da validade. Vencido é marcado EXPIRED e tratado como inexistente. */
    @Transactional
    public Optional<Consent> findValid(Long customerId) {
        Optional<Consent> active = consents.findByCustomerIdAndStatus(customerId, Consent.Status.ACTIVE);
        if (active.isPresent() && active.get().isExpired(clock.instant())) {
            active.get().expire();
            consents.flush();
            return Optional.empty();
        }
        return active;
    }

    @Transactional
    public Consent requireValid(Long customerId) {
        return findValid(customerId).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "CONSENT_REQUIRED",
                "Consentimento Open Insurance ativo é necessário"));
    }

    @Transactional
    public Consent getActive(Long customerId) {
        return findValid(customerId)
                .orElseThrow(() -> ApiException.notFound("CONSENT_NOT_FOUND", "Nenhum consentimento ativo"));
    }

    /** RN-07: revogar apaga o cache de apólices externas do cliente (LGPD). */
    @Transactional
    public Consent revoke(Long customerId) {
        Consent consent = getActive(customerId);
        consent.revoke(clock.instant());
        int removed = cache.deleteByCustomerId(customerId);
        audit.record(customerId, AuditAction.CONSENT_REVOKED, "CONSENT", consent.getId(),
                removed + " apólice(s) externa(s) removida(s) do cache");
        return consent;
    }
}
