package br.com.insurehub.quote;

import br.com.insurehub.common.Money;
import java.math.BigDecimal;
import java.math.MathContext;
import org.springframework.stereotype.Component;

/** Cálculo de prêmio da RN-03. Sem estado e sem I/O. */
@Component
public class PremiumCalculator {

    public static final BigDecimal PORTABILITY_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    public record Premium(BigDecimal baseRate, BigDecimal ageFactor, BigDecimal planMultiplier, BigDecimal discount,
                          BigDecimal annual, BigDecimal monthly) {}

    /**
     * @param amount importância segurada; em PREVIDENCIA, a contribuição mensal
     * @param tripDays dias de viagem (só VIAGEM)
     */
    public Premium calculate(ProductLine line, Plan plan, BigDecimal amount, Integer tripDays, int age,
                             BigDecimal discount) {
        if (line == ProductLine.PREVIDENCIA) {
            // Contribuição não tem taxa, fator de idade, plano nem desconto.
            return new Premium(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                    Money.round(amount.multiply(TWELVE)), Money.round(amount));
        }
        BigDecimal ageFactor = line.ageFactor(age);
        BigDecimal value = amount
                .multiply(line.baseRate())
                .multiply(BigDecimal.ONE.add(ageFactor))
                .multiply(plan.multiplier())
                .multiply(BigDecimal.ONE.subtract(discount));
        if (line == ProductLine.VIAGEM) {
            // Prêmio único: o mesmo valor em annualPremium e monthlyPremium.
            BigDecimal total = Money.round(value.multiply(BigDecimal.valueOf(tripDays)));
            return new Premium(line.baseRate(), ageFactor, plan.multiplier(), discount, total, total);
        }
        BigDecimal monthly = value.divide(TWELVE, MathContext.DECIMAL64);
        return new Premium(line.baseRate(), ageFactor, plan.multiplier(), discount, Money.round(value),
                Money.round(monthly));
    }
}
