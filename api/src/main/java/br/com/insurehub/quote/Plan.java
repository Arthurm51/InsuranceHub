package br.com.insurehub.quote;

import java.math.BigDecimal;

/**
 * Planos comerciais (RN-03). O multiplicador incide no prêmio dos produtos com taxa;
 * a franquia vale para Auto e a taxa de administração para Previdência.
 */
public enum Plan {

    ESSENCIAL("Essencial", "1.00", "0.06", "0.015"),
    COMPLETO("Completo", "1.25", "0.04", "0.010"),
    PREMIUM("Premium", "1.50", "0.02", "0.005");

    private final String label;
    private final BigDecimal multiplier;
    private final BigDecimal autoDeductibleRate;
    private final BigDecimal pensionAdminFee;

    Plan(String label, String multiplier, String autoDeductibleRate, String pensionAdminFee) {
        this.label = label;
        this.multiplier = new BigDecimal(multiplier);
        this.autoDeductibleRate = new BigDecimal(autoDeductibleRate);
        this.pensionAdminFee = new BigDecimal(pensionAdminFee);
    }

    public String label() { return label; }
    public BigDecimal multiplier() { return multiplier; }
    public BigDecimal autoDeductibleRate() { return autoDeductibleRate; }
    public BigDecimal pensionAdminFee() { return pensionAdminFee; }
}
