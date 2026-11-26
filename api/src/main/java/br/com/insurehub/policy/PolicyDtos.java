package br.com.insurehub.policy;

import br.com.insurehub.policy.details.PolicyDetailsService;
import br.com.insurehub.quote.Plan;
import br.com.insurehub.quote.ProductLine;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public final class PolicyDtos {

    private PolicyDtos() {}

    /** details: objeto com os dados do produto (formato por linha em docs/regras-de-negocio.md, RN-04). */
    public record IssueRequest(
            @NotNull(message = "quoteId é obrigatório") Long quoteId,
            LocalDate startDate,
            JsonNode details) {}

    public record CancelRequest(
            @NotBlank(message = "Motivo é obrigatório")
            @Size(min = 5, max = 255, message = "Motivo deve ter entre 5 e 255 caracteres")
            String reason) {}

    public record PolicyResponse(Long id, String number, ProductLine line, Plan plan, String planName,
                                 String insuredName, BigDecimal insuredAmount, BigDecimal monthlyContribution,
                                 BigDecimal annualPremium, BigDecimal monthlyPremium, Policy.Status status,
                                 LocalDate startDate, LocalDate endDate, Map<String, Object> details, Long quoteId,
                                 String cancellationReason, Instant cancelledAt, Instant createdAt) {

        public static PolicyResponse from(Policy p, LocalDate today) {
            Map<String, Object> details = PolicyDetailsService.forDisplay(p.getLine(), p.getDetails(),
                    p.getInsuredAmount(), p.getMonthlyContribution(), p.contributionsMade(today));
            return new PolicyResponse(p.getId(), p.getNumber(), p.getLine(), p.getPlan(), p.getPlanName(),
                    p.getInsuredName(), p.getInsuredAmount(), p.getMonthlyContribution(), p.getAnnualPremium(),
                    p.getMonthlyPremium(), p.getStatus(), p.getStartDate(), p.getEndDate(), details, p.getQuoteId(),
                    p.getCancellationReason(), p.getCancelledAt(), p.getCreatedAt());
        }
    }
}
