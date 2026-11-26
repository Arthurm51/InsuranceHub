package br.com.insurehub.claim;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class ClaimDtos {

    private ClaimDtos() {}

    public record OpenClaimRequest(
            @NotNull(message = "policyId é obrigatório") Long policyId,

            @NotBlank(message = "Tipo é obrigatório") String type,

            @NotNull(message = "Data de ocorrência é obrigatória") LocalDate occurrenceDate,

            @NotNull(message = "Valor é obrigatório")
            @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
            @Digits(integer = 12, fraction = 2, message = "Valor deve ter no máximo 2 casas decimais")
            BigDecimal amount,

            @NotBlank(message = "Descrição é obrigatória")
            @Size(min = 20, max = 1000, message = "Descrição deve ter entre 20 e 1000 caracteres")
            String description) {}

    public record ApproveRequest(
            @NotNull(message = "Valor aprovado é obrigatório")
            @DecimalMin(value = "0.01", message = "Valor aprovado deve ser maior que zero")
            @Digits(integer = 12, fraction = 2, message = "Valor aprovado deve ter no máximo 2 casas decimais")
            BigDecimal approvedAmount) {}

    public record RejectRequest(
            @NotBlank(message = "Motivo é obrigatório")
            @Size(min = 10, max = 500, message = "Motivo deve ter entre 10 e 500 caracteres")
            String reason) {}

    public record ClaimResponse(Long id, String protocol, Long policyId, String type, LocalDate occurrenceDate,
                                BigDecimal amount, BigDecimal approvedAmount, String description,
                                ClaimStatus status, String rejectionReason, Instant createdAt, Instant updatedAt) {

        public static ClaimResponse from(Claim c) {
            return new ClaimResponse(c.getId(), c.getProtocol(), c.getPolicyId(), c.getType(),
                    c.getOccurrenceDate(), c.getAmount(), c.getApprovedAmount(), c.getDescription(), c.getStatus(),
                    c.getRejectionReason(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
