package br.com.insurehub.claim;

import br.com.insurehub.audit.AuditAction;
import br.com.insurehub.audit.AuditService;
import br.com.insurehub.claim.ClaimDtos.OpenClaimRequest;
import br.com.insurehub.common.ApiException;
import br.com.insurehub.policy.Policy;
import br.com.insurehub.policy.PolicyRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {

    public static final int MAX_NOTICE_DAYS = 90;

    /** Transições permitidas pelo analista (RN-06). */
    private static final Map<ClaimStatus, Set<ClaimStatus>> ADMIN_TRANSITIONS = Map.of(
            ClaimStatus.OPEN, Set.of(ClaimStatus.IN_ANALYSIS),
            ClaimStatus.IN_ANALYSIS, Set.of(ClaimStatus.APPROVED, ClaimStatus.REJECTED),
            ClaimStatus.APPROVED, Set.of(ClaimStatus.PAID));

    private final ClaimRepository claims;
    private final PolicyRepository policies;
    private final AuditService audit;
    private final Clock clock;

    public ClaimService(ClaimRepository claims, PolicyRepository policies, AuditService audit, Clock clock) {
        this.claims = claims;
        this.policies = policies;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Claim open(Long customerId, OpenClaimRequest req) {
        Policy policy = policies.findByIdAndCustomerId(req.policyId(), customerId)
                .orElseThrow(() -> ApiException.notFound("POLICY_NOT_FOUND", "Apólice não encontrada"));
        if (!policy.isActive()) {
            throw ApiException.conflict("POLICY_NOT_ACTIVE", "Apólice não está ativa");
        }
        String type = req.type().trim().toUpperCase(java.util.Locale.ROOT);
        if (!policy.getLine().covers(type)) {
            throw ApiException.unprocessable("CLAIM_TYPE_NOT_COVERED",
                    "Tipo " + type + " não é coberto pela linha " + policy.getLine()
                            + ". Aceitos: " + policy.getLine().claimTypes());
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate occurrence = req.occurrenceDate();
        if (occurrence.isAfter(today) || occurrence.isBefore(today.minusDays(MAX_NOTICE_DAYS))
                || occurrence.isBefore(policy.getStartDate()) || occurrence.isAfter(policy.getEndDate())) {
            throw ApiException.unprocessable("INVALID_OCCURRENCE_DATE",
                    "Data de ocorrência deve estar na vigência, não ser futura e ter no máximo "
                            + MAX_NOTICE_DAYS + " dias");
        }
        if (claims.existsByPolicyIdAndStatusIn(policy.getId(), ClaimStatus.PENDING)) {
            throw ApiException.conflict("CLAIM_ALREADY_OPEN", "Já existe sinistro em aberto para esta apólice");
        }
        BigDecimal amount = req.amount().setScale(2);
        if (amount.compareTo(remainingCoverage(policy)) > 0) {
            throw ApiException.unprocessable("AMOUNT_EXCEEDS_COVERAGE",
                    "Valor excede o saldo de cobertura de " + remainingCoverage(policy));
        }
        Instant now = clock.instant();
        String protocol = "SIN-%d-%06d".formatted(today.getYear(), claims.nextProtocol());
        Claim claim = claims.save(new Claim(protocol, policy.getId(), customerId, type, occurrence, amount,
                req.description().trim(), now));
        audit.record(customerId, AuditAction.CLAIM_OPENED, "CLAIM", claim.getId(), protocol);
        return claim;
    }

    @Transactional(readOnly = true)
    public List<Claim> listForCustomer(Long customerId) {
        return claims.findByCustomerIdOrderByIdDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Claim getForCustomer(Long customerId, Long claimId) {
        return claims.findByIdAndCustomerId(claimId, customerId)
                .orElseThrow(() -> ApiException.notFound("CLAIM_NOT_FOUND", "Sinistro não encontrado"));
    }

    @Transactional
    public Claim cancelByCustomer(Long customerId, Long claimId) {
        Claim claim = getForCustomer(customerId, claimId);
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw invalidTransition(claim.getStatus(), ClaimStatus.CANCELLED);
        }
        return transition(customerId, claim, ClaimStatus.CANCELLED);
    }

    // ---- Analista (ADMIN) ----

    @Transactional(readOnly = true)
    public List<Claim> listAll(ClaimStatus status) {
        return status == null ? claims.findAllByOrderByIdDesc() : claims.findByStatusOrderByIdDesc(status);
    }

    @Transactional(readOnly = true)
    public Claim get(Long claimId) {
        return claims.findById(claimId)
                .orElseThrow(() -> ApiException.notFound("CLAIM_NOT_FOUND", "Sinistro não encontrado"));
    }

    @Transactional
    public Claim startAnalysis(Long adminId, Long claimId) {
        Claim claim = get(claimId);
        requireAdminTransition(claim, ClaimStatus.IN_ANALYSIS);
        return transition(adminId, claim, ClaimStatus.IN_ANALYSIS);
    }

    @Transactional
    public Claim approve(Long adminId, Long claimId, BigDecimal approvedAmount) {
        Claim claim = get(claimId);
        requireAdminTransition(claim, ClaimStatus.APPROVED);
        BigDecimal approved = approvedAmount.setScale(2);
        if (approved.compareTo(claim.getAmount()) > 0) {
            throw ApiException.unprocessable("APPROVED_AMOUNT_EXCEEDS_CLAIM",
                    "Valor aprovado não pode exceder o valor solicitado");
        }
        Policy policy = policies.findById(claim.getPolicyId()).orElseThrow();
        if (approved.compareTo(remainingCoverage(policy)) > 0) {
            throw ApiException.unprocessable("AMOUNT_EXCEEDS_COVERAGE",
                    "Valor aprovado excede o saldo de cobertura de " + remainingCoverage(policy));
        }
        claim.approve(approved, clock.instant());
        audit.record(adminId, AuditAction.CLAIM_STATUS_CHANGED, "CLAIM", claim.getId(),
                "IN_ANALYSIS -> APPROVED (" + approved.toPlainString() + ")");
        return claim;
    }

    @Transactional
    public Claim reject(Long adminId, Long claimId, String reason) {
        Claim claim = get(claimId);
        requireAdminTransition(claim, ClaimStatus.REJECTED);
        claim.reject(reason.trim(), clock.instant());
        audit.record(adminId, AuditAction.CLAIM_STATUS_CHANGED, "CLAIM", claim.getId(), "IN_ANALYSIS -> REJECTED");
        return claim;
    }

    @Transactional
    public Claim pay(Long adminId, Long claimId) {
        Claim claim = get(claimId);
        requireAdminTransition(claim, ClaimStatus.PAID);
        return transition(adminId, claim, ClaimStatus.PAID);
    }

    /** Importância segurada menos o que já foi aprovado/pago. */
    public BigDecimal remainingCoverage(Policy policy) {
        BigDecimal used = claims.sumApprovedAmount(policy.getId(), ClaimStatus.CONSUMING_COVERAGE);
        return policy.getInsuredAmount().subtract(used);
    }

    private void requireAdminTransition(Claim claim, ClaimStatus target) {
        if (!ADMIN_TRANSITIONS.getOrDefault(claim.getStatus(), Set.of()).contains(target)) {
            throw invalidTransition(claim.getStatus(), target);
        }
    }

    private Claim transition(Long actorId, Claim claim, ClaimStatus target) {
        ClaimStatus from = claim.getStatus();
        claim.changeStatus(target, clock.instant());
        audit.record(actorId, AuditAction.CLAIM_STATUS_CHANGED, "CLAIM", claim.getId(), from + " -> " + target);
        return claim;
    }

    private ApiException invalidTransition(ClaimStatus from, ClaimStatus to) {
        return ApiException.conflict("INVALID_CLAIM_TRANSITION",
                "Transição de " + from + " para " + to + " não permitida");
    }
}
