package br.com.insurehub.quote;

import br.com.insurehub.audit.AuditAction;
import br.com.insurehub.audit.AuditService;
import br.com.insurehub.common.ApiError.FieldViolation;
import br.com.insurehub.common.ApiException;
import br.com.insurehub.common.ValidationException;
import br.com.insurehub.quote.QuoteDtos.QuoteRequest;
import br.com.insurehub.user.User;
import br.com.insurehub.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteService {

    public static final int VALIDITY_DAYS = 7;
    public static final int MIN_TRIP_DAYS = 1;
    public static final int MAX_TRIP_DAYS = 90;

    private final QuoteRepository quotes;
    private final UserService userService;
    private final PremiumCalculator calculator;
    private final AuditService audit;
    private final Clock clock;

    public QuoteService(QuoteRepository quotes, UserService userService, PremiumCalculator calculator,
                        AuditService audit, Clock clock) {
        this.quotes = quotes;
        this.userService = userService;
        this.calculator = calculator;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Quote create(Long customerId, QuoteRequest req) {
        validateConditionalFields(req);
        BigDecimal amount = req.line().isContributionBased() ? req.monthlyContribution() : req.insuredAmount();
        Quote quote = build(customerId, req.line(), req.plan(), amount, req.tripDays(), BigDecimal.ZERO,
                Quote.Origin.DIRECT, null);
        audit.record(customerId, AuditAction.QUOTE_CREATED, "QUOTE", quote.getId(),
                req.line() + " " + req.plan() + " " + amount.toPlainString());
        return quote;
    }

    /** Usado pela portabilidade (RN-09): mesmas regras da RN-03, com o desconto informado. */
    @Transactional
    public Quote createPortability(Long customerId, ProductLine line, Plan plan, BigDecimal amount,
                                   BigDecimal discount, String externalPolicyId) {
        return build(customerId, line, plan, amount, null, discount, Quote.Origin.PORTABILITY, externalPolicyId);
    }

    @Transactional(readOnly = true)
    public List<Quote> list(Long customerId) {
        return quotes.findByCustomerIdOrderByIdDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Quote get(Long customerId, Long quoteId) {
        return quotes.findByIdAndCustomerId(quoteId, customerId)
                .orElseThrow(() -> ApiException.notFound("QUOTE_NOT_FOUND", "Cotação não encontrada"));
    }

    private void validateConditionalFields(QuoteRequest req) {
        List<FieldViolation> errors = new ArrayList<>();
        if (req.line().isContributionBased()) {
            if (req.monthlyContribution() == null) {
                errors.add(new FieldViolation("monthlyContribution", "Contribuição mensal é obrigatória para Previdência"));
            }
            if (req.insuredAmount() != null) {
                errors.add(new FieldViolation("insuredAmount", "Previdência não usa importância segurada"));
            }
        } else {
            if (req.insuredAmount() == null) {
                errors.add(new FieldViolation("insuredAmount", "Importância segurada é obrigatória"));
            }
            if (req.monthlyContribution() != null) {
                errors.add(new FieldViolation("monthlyContribution", "Contribuição mensal só se aplica a Previdência"));
            }
        }
        if (req.line() == ProductLine.VIAGEM) {
            if (req.tripDays() == null) {
                errors.add(new FieldViolation("tripDays", "Dias de viagem são obrigatórios para Viagem"));
            } else if (req.tripDays() < MIN_TRIP_DAYS || req.tripDays() > MAX_TRIP_DAYS) {
                errors.add(new FieldViolation("tripDays", "Dias de viagem devem estar entre 1 e 90"));
            }
        } else if (req.tripDays() != null) {
            errors.add(new FieldViolation("tripDays", "Dias de viagem só se aplicam a Viagem"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private Quote build(Long customerId, ProductLine line, Plan plan, BigDecimal rawAmount, Integer tripDays,
                        BigDecimal discount, Quote.Origin origin, String externalPolicyId) {
        User customer = userService.get(customerId);
        BigDecimal amount = rawAmount.setScale(2);
        if (!line.acceptsAmount(amount)) {
            String what = line.isContributionBased() ? "Contribuição mensal" : "Importância segurada";
            throw ApiException.unprocessable("INSURED_AMOUNT_OUT_OF_RANGE",
                    what + " para " + line.label() + " deve estar entre " + line.minAmount()
                            + " e " + line.maxAmount());
        }
        int age = Period.between(customer.getBirthDate(), LocalDate.now(clock)).getYears();
        if (!line.acceptsAge(age)) {
            throw ApiException.unprocessable("AGE_NOT_ELIGIBLE",
                    "Idade " + age + " não é elegível para " + line.label());
        }
        PremiumCalculator.Premium premium = calculator.calculate(line, plan, amount, tripDays, age, discount);
        Instant now = clock.instant();
        return quotes.save(new Quote(customerId, line, plan, amount, tripDays, age, premium, origin,
                externalPolicyId, now, now.plus(VALIDITY_DAYS, ChronoUnit.DAYS)));
    }

    public Instant now() {
        return clock.instant();
    }
}
