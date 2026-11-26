package br.com.insurehub.quote;

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

@Entity
@Table(name = "quotes")
public class Quote {

    public enum Status { PENDING, CONVERTED }

    public enum Origin { DIRECT, PORTABILITY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

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

    @Column(name = "trip_days")
    private Integer tripDays;

    @Column(name = "customer_age", nullable = false)
    private int customerAge;

    @Column(name = "base_rate", nullable = false)
    private BigDecimal baseRate;

    @Column(name = "age_factor", nullable = false)
    private BigDecimal ageFactor;

    @Column(name = "plan_multiplier", nullable = false)
    private BigDecimal planMultiplier;

    @Column(nullable = false)
    private BigDecimal discount;

    @Column(name = "annual_premium", nullable = false)
    private BigDecimal annualPremium;

    @Column(name = "monthly_premium", nullable = false)
    private BigDecimal monthlyPremium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Origin origin;

    @Column(name = "external_policy_id")
    private String externalPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected Quote() {}

    public Quote(Long customerId, ProductLine line, Plan plan, BigDecimal amount, Integer tripDays, int customerAge,
                 PremiumCalculator.Premium premium, Origin origin, String externalPolicyId,
                 Instant createdAt, Instant expiresAt) {
        this.customerId = customerId;
        this.line = line;
        this.plan = plan;
        if (line.isContributionBased()) {
            this.monthlyContribution = amount;
        } else {
            this.insuredAmount = amount;
        }
        this.tripDays = tripDays;
        this.customerAge = customerAge;
        this.baseRate = premium.baseRate();
        this.ageFactor = premium.ageFactor();
        this.planMultiplier = premium.planMultiplier();
        this.discount = premium.discount();
        this.annualPremium = premium.annual();
        this.monthlyPremium = premium.monthly();
        this.origin = origin;
        this.externalPolicyId = externalPolicyId;
        this.status = Status.PENDING;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return status == Status.PENDING && now.isAfter(expiresAt);
    }

    public void markConverted() {
        this.status = Status.CONVERTED;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public ProductLine getLine() { return line; }
    public Plan getPlan() { return plan; }
    public BigDecimal getInsuredAmount() { return insuredAmount; }
    public BigDecimal getMonthlyContribution() { return monthlyContribution; }
    public Integer getTripDays() { return tripDays; }
    public int getCustomerAge() { return customerAge; }
    public BigDecimal getBaseRate() { return baseRate; }
    public BigDecimal getAgeFactor() { return ageFactor; }
    public BigDecimal getPlanMultiplier() { return planMultiplier; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getAnnualPremium() { return annualPremium; }
    public BigDecimal getMonthlyPremium() { return monthlyPremium; }
    public Origin getOrigin() { return origin; }
    public String getExternalPolicyId() { return externalPolicyId; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
