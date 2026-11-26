package br.com.insurehub.policy;

import br.com.insurehub.audit.AuditAction;
import br.com.insurehub.audit.AuditService;
import br.com.insurehub.claim.ClaimRepository;
import br.com.insurehub.claim.ClaimStatus;
import br.com.insurehub.common.ApiException;
import br.com.insurehub.policy.details.PolicyDetailsService;
import br.com.insurehub.quote.ProductLine;
import br.com.insurehub.quote.Quote;
import br.com.insurehub.quote.QuoteService;
import br.com.insurehub.user.User;
import br.com.insurehub.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    public static final int MAX_START_DAYS_AHEAD = 30;
    public static final int MAX_TRIP_START_DAYS_AHEAD = 180;
    public static final int PENSION_TERM_YEARS = 30;

    private final PolicyRepository policies;
    private final QuoteService quoteService;
    private final ClaimRepository claims;
    private final PolicyDetailsService detailsService;
    private final UserService userService;
    private final AuditService audit;
    private final Clock clock;

    public PolicyService(PolicyRepository policies, QuoteService quoteService, ClaimRepository claims,
                         PolicyDetailsService detailsService, UserService userService, AuditService audit,
                         Clock clock) {
        this.policies = policies;
        this.quoteService = quoteService;
        this.claims = claims;
        this.detailsService = detailsService;
        this.userService = userService;
        this.audit = audit;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @Transactional
    public Policy issue(Long customerId, Long quoteId, LocalDate requestedStart, JsonNode rawDetails) {
        Quote quote = quoteService.get(customerId, quoteId);
        Instant now = clock.instant();
        if (quote.getStatus() == Quote.Status.CONVERTED) {
            throw ApiException.conflict("QUOTE_ALREADY_CONVERTED", "Cotação já foi convertida em apólice");
        }
        if (quote.isExpired(now)) {
            throw ApiException.unprocessable("QUOTE_EXPIRED", "Cotação expirada");
        }
        ProductLine line = quote.getLine();
        LocalDate today = LocalDate.now(clock);
        LocalDate start = requestedStart == null ? today : requestedStart;
        int maxAhead = line == ProductLine.VIAGEM ? MAX_TRIP_START_DAYS_AHEAD : MAX_START_DAYS_AHEAD;
        if (start.isBefore(today) || start.isAfter(today.plusDays(maxAhead))) {
            throw ApiException.unprocessable("INVALID_START_DATE",
                    "Início de vigência deve estar entre hoje e " + maxAhead + " dias à frente");
        }
        if (policies.existsByCustomerIdAndLineAndStatus(customerId, line, Policy.Status.ACTIVE)) {
            throw ApiException.conflict("ACTIVE_POLICY_ALREADY_EXISTS",
                    "Cliente já possui apólice ativa de " + line.label());
        }
        Map<String, Object> details = detailsService.validate(quote, rawDetails);
        LocalDate end = switch (line) {
            case VIAGEM -> start.plusDays(quote.getTripDays() - 1L);
            case PREVIDENCIA -> start.plusYears(PENSION_TERM_YEARS).minusDays(1);
            default -> start.plusYears(1).minusDays(1);
        };
        User customer = userService.get(customerId);
        String number = "IH-%s-%d-%06d".formatted(line.prefix(), today.getYear(), policies.nextNumber());
        Policy policy = policies.save(new Policy(number, customerId, customer.getName(), quote.getId(), line,
                quote.getPlan(), quote.getInsuredAmount(), quote.getMonthlyContribution(), quote.getAnnualPremium(),
                quote.getMonthlyPremium(), details, start, end, now));
        quote.markConverted();
        audit.record(customerId, AuditAction.POLICY_ISSUED, "POLICY", policy.getId(), number);
        return policy;
    }

    @Transactional(readOnly = true)
    public List<Policy> list(Long customerId, Policy.Status status) {
        return status == null
                ? policies.findByCustomerIdOrderByIdDesc(customerId)
                : policies.findByCustomerIdAndStatusOrderByIdDesc(customerId, status);
    }

    @Transactional(readOnly = true)
    public Policy get(Long customerId, Long policyId) {
        return policies.findByIdAndCustomerId(policyId, customerId)
                .orElseThrow(() -> ApiException.notFound("POLICY_NOT_FOUND", "Apólice não encontrada"));
    }

    @Transactional
    public Policy cancel(Long customerId, Long policyId, String reason) {
        Policy policy = get(customerId, policyId);
        if (!policy.isActive()) {
            throw ApiException.conflict("POLICY_NOT_ACTIVE", "Apólice não está ativa");
        }
        if (claims.existsByPolicyIdAndStatusIn(policyId, ClaimStatus.PENDING)) {
            throw ApiException.conflict("POLICY_HAS_PENDING_CLAIMS",
                    "Apólice possui sinistro em aberto ou em análise");
        }
        policy.cancel(reason.trim(), clock.instant());
        audit.record(customerId, AuditAction.POLICY_CANCELLED, "POLICY", policy.getId(), reason.trim());
        return policy;
    }

    /** RN-05: apólices ativas com fim de vigência no passado passam a EXPIRED. */
    @Transactional
    public int expireOverdue() {
        List<Policy> overdue = policies.findByStatusAndEndDateBefore(Policy.Status.ACTIVE, LocalDate.now(clock));
        for (Policy p : overdue) {
            p.expire();
            audit.record(p.getCustomerId(), AuditAction.POLICY_EXPIRED, "POLICY", p.getId(), p.getNumber());
        }
        return overdue.size();
    }
}
