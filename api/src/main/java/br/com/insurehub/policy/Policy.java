package br.com.insurehub.policy;

import br.com.insurehub.quote.Plan;
import br.com.insurehub.quote.ProductLine;
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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policies")
public class Policy {

    public enum Status { ACTIVE, CANCELLED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "quote_id", nullable = false, unique = true)
    private Long quoteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductLine line;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Column(name = "insured_amount")
    private BigDecimal insuredAmount;

    @Column(name = "monthly_contribution")
    private BigDecimal monthlyContribution;

    @Column(name = "annual_premium", nullable = false)
    private BigDecimal annualPremium;

    @Column(name = "monthly_premium", nullable = false)
    private BigDecimal monthlyPremium;

    @Column(name = "insured_name", nullable = false)
    private String insuredName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Policy() {}

    public Policy(String number, Long customerId, String insuredName, Long quoteId, ProductLine line, Plan plan,
                  BigDecimal insuredAmount, BigDecimal monthlyContribution, BigDecimal annualPremium,
                  BigDecimal monthlyPremium, Map<String, Object> details, LocalDate startDate, LocalDate endDate,
                  Instant createdAt) {
        this.number = number;
        this.customerId = customerId;
        this.insuredName = insuredName;
        this.quoteId = quoteId;
        this.line = line;
        this.plan = plan;
        this.insuredAmount = insuredAmount;
        this.monthlyContribution = monthlyContribution;
        this.annualPremium = annualPremium;
        this.monthlyPremium = monthlyPremium;
        this.details = details;
        this.status = Status.ACTIVE;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
    }

    public void cancel(String reason, Instant when) {
        this.status = Status.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = when;
    }

    public void expire() {
        this.status = Status.EXPIRED;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    /** Previdência: contribuições já feitas = meses desde o início, contando o mês inicial. */
    public long contributionsMade(LocalDate today) {
        if (today.isBefore(startDate)) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(startDate, today) + 1;
    }

    public String getPlanName() {
        return line.planName(plan);
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public Long getCustomerId() { return customerId; }
    public Long getQuoteId() { return quoteId; }
    public ProductLine getLine() { return line; }
    public Plan getPlan() { return plan; }
    public BigDecimal getInsuredAmount() { return insuredAmount; }
    public BigDecimal getMonthlyContribution() { return monthlyContribution; }
    public BigDecimal getAnnualPremium() { return annualPremium; }
    public BigDecimal getMonthlyPremium() { return monthlyPremium; }
    public String getInsuredName() { return insuredName; }
    public Map<String, Object> getDetails() { return details; }
    public Status getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getCancellationReason() { return cancellationReason; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
}
