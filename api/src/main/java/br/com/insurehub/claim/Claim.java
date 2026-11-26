package br.com.insurehub.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String protocol;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String type;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Claim() {}

    public Claim(String protocol, Long policyId, Long customerId, String type, LocalDate occurrenceDate,
                 BigDecimal amount, String description, Instant now) {
        this.protocol = protocol;
        this.policyId = policyId;
        this.customerId = customerId;
        this.type = type;
        this.occurrenceDate = occurrenceDate;
        this.amount = amount;
        this.description = description;
        this.status = ClaimStatus.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changeStatus(ClaimStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    public void approve(BigDecimal approved, Instant now) {
        this.approvedAmount = approved;
        changeStatus(ClaimStatus.APPROVED, now);
    }

    public void reject(String reason, Instant now) {
        this.rejectionReason = reason;
        changeStatus(ClaimStatus.REJECTED, now);
    }

    public Long getId() { return id; }
    public String getProtocol() { return protocol; }
    public Long getPolicyId() { return policyId; }
    public Long getCustomerId() { return customerId; }
    public String getType() { return type; }
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public String getDescription() { return description; }
    public ClaimStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
