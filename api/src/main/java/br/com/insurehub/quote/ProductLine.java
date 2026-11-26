package br.com.insurehub.quote;

import java.math.BigDecimal;
import java.util.Set;

/** Parâmetros comerciais de cada produto (RN-03 e RN-06). */
public enum ProductLine {

    AUTO("AUT", "Auto", "10000.00", "500000.00", "0.0400", 18, 80,
            Set.of("COLISAO", "ROUBO_FURTO", "DANOS_TERCEIROS")) {
        @Override
        public BigDecimal ageFactor(int age) {
            if (age <= 25) return new BigDecimal("0.30");
            if (age <= 60) return BigDecimal.ZERO;
            return new BigDecimal("0.15");
        }
    },

    RESIDENCIAL("RES", "Residencial", "50000.00", "2000000.00", "0.0050", 18, null,
            Set.of("INCENDIO", "DANOS_ELETRICOS", "ROUBO_FURTO")) {
        @Override
        public BigDecimal ageFactor(int age) {
            return BigDecimal.ZERO;
        }
    },

    VIDA("VID", "Vida", "20000.00", "1000000.00", "0.0120", 18, 70,
            Set.of("MORTE", "INVALIDEZ")) {
        @Override
        public BigDecimal ageFactor(int age) {
            return age >= 50 ? new BigDecimal("0.40") : BigDecimal.ZERO;
        }
    },

    /** Faixa de valores = contribuição mensal. Sem taxa: o "prêmio" é a própria contribuição. */
    PREVIDENCIA("PRV", "Previdência", "100.00", "50000.00", null, 18, 75, Set.of()) {
        @Override
        public BigDecimal ageFactor(int age) {
            return BigDecimal.ZERO;
        }
    },

    /** Taxa por dia de viagem, aplicada sobre a cobertura médica. */
    VIAGEM("VIA", "Viagem", "30000.00", "1000000.00", "0.0004", 18, 85,
            Set.of("DESPESAS_MEDICAS", "EXTRAVIO_BAGAGEM", "CANCELAMENTO_VIAGEM")) {
        @Override
        public BigDecimal ageFactor(int age) {
            return age >= 60 ? new BigDecimal("0.50") : BigDecimal.ZERO;
        }
    };

    private final String prefix;
    private final String label;
    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;
    private final BigDecimal baseRate;
    private final int minAge;
    private final Integer maxAge;
    private final Set<String> claimTypes;

    ProductLine(String prefix, String label, String min, String max, String baseRate, int minAge, Integer maxAge,
                Set<String> claimTypes) {
        this.prefix = prefix;
        this.label = label;
        this.minAmount = new BigDecimal(min);
        this.maxAmount = new BigDecimal(max);
        this.baseRate = baseRate == null ? BigDecimal.ZERO : new BigDecimal(baseRate);
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.claimTypes = claimTypes;
    }

    public abstract BigDecimal ageFactor(int age);

    public boolean acceptsAmount(BigDecimal amount) {
        return amount.compareTo(minAmount) >= 0 && amount.compareTo(maxAmount) <= 0;
    }

    public boolean acceptsAge(int age) {
        return age >= minAge && (maxAge == null || age <= maxAge);
    }

    public boolean covers(String claimType) {
        return claimTypes.contains(claimType);
    }

    public boolean isContributionBased() {
        return this == PREVIDENCIA;
    }

    public String planName(Plan plan) {
        return label + " " + plan.label();
    }

    public String prefix() { return prefix; }
    public String label() { return label; }
    public BigDecimal minAmount() { return minAmount; }
    public BigDecimal maxAmount() { return maxAmount; }
    public BigDecimal baseRate() { return baseRate; }
    public int minAge() { return minAge; }
    public Integer maxAge() { return maxAge; }
    public Set<String> claimTypes() { return claimTypes; }
}
