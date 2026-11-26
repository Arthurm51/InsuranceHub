package br.com.insurehub.portability;

import br.com.insurehub.audit.AuditAction;
import br.com.insurehub.audit.AuditService;
import br.com.insurehub.common.ApiException;
import br.com.insurehub.opin.ConsentService;
import br.com.insurehub.opin.ExternalPolicy;
import br.com.insurehub.opin.ExternalPolicyRepository;
import br.com.insurehub.quote.Plan;
import br.com.insurehub.quote.PremiumCalculator;
import br.com.insurehub.quote.ProductLine;
import br.com.insurehub.quote.Quote;
import java.math.BigDecimal;
import br.com.insurehub.quote.QuoteRepository;
import br.com.insurehub.quote.QuoteService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortabilityService {

    public static final int RENEWAL_WINDOW_DAYS = 60;

    private final ConsentService consentService;
    private final ExternalPolicyRepository externalPolicies;
    private final QuoteRepository quotes;
    private final QuoteService quoteService;
    private final AuditService audit;
    private final Clock clock;

    public PortabilityService(ConsentService consentService, ExternalPolicyRepository externalPolicies,
                              QuoteRepository quotes, QuoteService quoteService, AuditService audit, Clock clock) {
        this.consentService = consentService;
        this.externalPolicies = externalPolicies;
        this.quotes = quotes;
        this.quoteService = quoteService;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Quote requestQuote(Long customerId, String externalPolicyId) {
        consentService.requireValid(customerId);
        ExternalPolicy external = externalPolicies.findByCustomerIdAndExternalId(customerId, externalPolicyId)
                .orElseThrow(() -> ApiException.notFound("EXTERNAL_POLICY_NOT_FOUND",
                        "Apólice externa não encontrada no cache. Consulte /opin/external-policies primeiro"));
        ProductLine line = Arrays.stream(ProductLine.values())
                .filter(l -> l.name().equals(external.getLine()) && l != ProductLine.VIAGEM)
                .findFirst()
                .orElseThrow(() -> ApiException.unprocessable("LINE_NOT_SUPPORTED",
                        "Produto " + external.getLine() + " não pode ser portado para a InsureHub"));
        LocalDate today = LocalDate.now(clock);
        // Previdência pode ser portada a qualquer momento; seguros, só perto do vencimento.
        if (line != ProductLine.PREVIDENCIA && (external.getEndDate().isBefore(today)
                || external.getEndDate().isAfter(today.plusDays(RENEWAL_WINDOW_DAYS)))) {
            throw ApiException.unprocessable("OUTSIDE_RENEWAL_WINDOW",
                    "Portabilidade só é permitida para apólices que vencem nos próximos " + RENEWAL_WINDOW_DAYS
                            + " dias");
        }
        if (quotes.existsByCustomerIdAndExternalPolicyId(customerId, externalPolicyId)) {
            throw ApiException.conflict("ALREADY_PORTED", "Esta apólice externa já gerou cotação de portabilidade");
        }
        Quote quote;
        if (line == ProductLine.PREVIDENCIA) {
            Object contribution = external.getDetails().get("monthlyContribution");
            if (contribution == null) {
                throw ApiException.unprocessable("EXTERNAL_DATA_INCOMPLETE",
                        "Apólice externa não informa a contribuição mensal");
            }
            quote = quoteService.createPortability(customerId, line, Plan.PREMIUM,
                    new BigDecimal(contribution.toString()), BigDecimal.ZERO, externalPolicyId);
        } else {
            quote = quoteService.createPortability(customerId, line, Plan.ESSENCIAL, external.getInsuredAmount(),
                    PremiumCalculator.PORTABILITY_DISCOUNT, externalPolicyId);
        }
        audit.record(customerId, AuditAction.PORTABILITY_QUOTE_CREATED, "QUOTE", quote.getId(),
                external.getInsurer() + " " + external.getPolicyNumber());
        return quote;
    }
}
