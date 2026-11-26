package br.com.insurehub.opin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Cópia local (cache) de uma apólice de congênere obtida via OPIN. */
@Entity
@Table(name = "external_policies_cache")
public class ExternalPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String insurer;

    @Column(nullable = false)
    private String line;

    @Column(name = "policy_number", nullable = false)
    private String policyNumber;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "insured_name")
    private String insuredName;

    @Column(name = "insured_amount")
    private BigDecimal insuredAmount;

    @Column(name = "annual_premium", nullable = false)
    private BigDecimal annualPremium;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected ExternalPolicy() {}

    public ExternalPolicy(Long customerId, OpinClient.OpinPolicy p, Instant fetchedAt) {
        this.customerId = customerId;
        this.externalId = p.policyId();
        this.insurer = p.insurer();
        this.line = p.line();
        this.policyNumber = p.policyNumber();
        this.planName = p.planName();
        this.insuredName = p.insuredName();
        this.insuredAmount = p.insuredAmount();
        this.annualPremium = p.annualPremium();
        this.startDate = p.startDate();
        this.endDate = p.endDate();
        this.details = p.details() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(p.details());
        this.fetchedAt = fetchedAt;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getExternalId() { return externalId; }
    public String getInsurer() { return insurer; }
    public String getLine() { return line; }
    public String getPolicyNumber() { return policyNumber; }
    public String getPlanName() { return planName; }
    public String getInsuredName() { return insuredName; }
    public BigDecimal getInsuredAmount() { return insuredAmount; }
    public BigDecimal getAnnualPremium() { return annualPremium; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Map<String, Object> getDetails() { return details; }
    public Instant getFetchedAt() { return fetchedAt; }
}
