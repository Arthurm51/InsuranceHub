package br.com.insurehub.quote;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public final class QuoteDtos {

    private QuoteDtos() {}

    /**
     * insuredAmount: obrigatório exceto em PREVIDENCIA.
     * monthlyContribution: obrigatório só em PREVIDENCIA.
     * tripDays: obrigatório só em VIAGEM.
     */
    public record QuoteRequest(
            @NotNull(message = "Produto é obrigatório")
            ProductLine line,

            @NotNull(message = "Plano é obrigatório")
            Plan plan,

            @DecimalMin(value = "0.01", message = "Importância segurada deve ser maior que zero")
            @Digits(integer = 12, fraction = 2, message = "Importância segurada deve ter no máximo 2 casas decimais")
            BigDecimal insuredAmount,

            @DecimalMin(value = "0.01", message = "Contribuição mensal deve ser maior que zero")
            @Digits(integer = 12, fraction = 2, message = "Contribuição mensal deve ter no máximo 2 casas decimais")
            BigDecimal monthlyContribution,

            Integer tripDays) {}

    /** status efetivo: PENDING, CONVERTED ou EXPIRED (PENDING com validade vencida). */
    public record QuoteResponse(Long id, ProductLine line, Plan plan, String planName, BigDecimal insuredAmount,
                                BigDecimal monthlyContribution, Integer tripDays, int customerAge,
                                BigDecimal baseRate, BigDecimal ageFactor, BigDecimal planMultiplier,
                                BigDecimal discount, BigDecimal annualPremium, BigDecimal monthlyPremium,
                                String origin, String externalPolicyId, String status, Instant createdAt,
                                Instant expiresAt) {

        public static QuoteResponse from(Quote q, Instant now) {
            String status = q.isExpired(now) ? "EXPIRED" : q.getStatus().name();
            return new QuoteResponse(q.getId(), q.getLine(), q.getPlan(), q.getLine().planName(q.getPlan()),
                    q.getInsuredAmount(), q.getMonthlyContribution(), q.getTripDays(), q.getCustomerAge(),
                    q.getBaseRate(), q.getAgeFactor(), q.getPlanMultiplier(), q.getDiscount(), q.getAnnualPremium(),
                    q.getMonthlyPremium(), q.getOrigin().name(), q.getExternalPolicyId(), status, q.getCreatedAt(),
                    q.getExpiresAt());
        }
    }
}
