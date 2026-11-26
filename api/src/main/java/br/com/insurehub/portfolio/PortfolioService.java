package br.com.insurehub.portfolio;

import br.com.insurehub.common.ApiException;
import br.com.insurehub.opin.ConsentService;
import br.com.insurehub.opin.ExternalPolicy;
import br.com.insurehub.opin.ExternalPolicyService;
import br.com.insurehub.policy.Policy;
import br.com.insurehub.policy.PolicyRepository;
import br.com.insurehub.policy.details.PolicyDetailsService;
import br.com.insurehub.quote.ProductLine;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Visão consolidada da carteira (RN-11 e RN-12): apólices ACTIVE da InsureHub + apólices vigentes
 * compartilhadas via OPIN, agrupadas pelos 5 produtos.
 */
@Service
public class PortfolioService {

    /** Ordem de exibição na Home. */
    public static final List<ProductLine> DISPLAY_ORDER = List.of(
            ProductLine.VIDA, ProductLine.AUTO, ProductLine.RESIDENCIAL, ProductLine.PREVIDENCIA, ProductLine.VIAGEM);

    public static final int MAX_QUERY_LENGTH = 60;

    public enum Holding { NONE, INSUREHUB, OPIN, INSUREHUB_OPIN }

    public enum Source { INSUREHUB, OPIN }

    public record OpinState(boolean consent, boolean available, ExternalPolicyService.Source source, boolean stale,
                            Instant syncedAt) {}

    public record ProductSummary(ProductLine line, String label, Holding holding, int insureHubCount,
                                 int opinCount) {}

    public record Overview(int totalProducts, int protectedCount, OpinState opin, List<ProductSummary> products) {}

    public record Item(Source source, String institution, String policyNumber, String planName, String insuredName,
                       LocalDate startDate, LocalDate endDate, Map<String, Object> details) {}

    public record ProductView(ProductLine line, String label, OpinState opin, int total, List<Item> items) {}

    private record Snapshot(OpinState opin, List<Policy> own, List<ExternalPolicy> shared) {}

    private final PolicyRepository policies;
    private final ConsentService consentService;
    private final ExternalPolicyService externalPolicyService;
    private final Clock clock;

    public PortfolioService(PolicyRepository policies, ConsentService consentService,
                            ExternalPolicyService externalPolicyService, Clock clock) {
        this.policies = policies;
        this.consentService = consentService;
        this.externalPolicyService = externalPolicyService;
        this.clock = clock;
    }

    public Overview overview(Long customerId) {
        Snapshot s = snapshot(customerId);
        List<ProductSummary> products = DISPLAY_ORDER.stream().map(line -> {
            int own = (int) s.own().stream().filter(p -> p.getLine() == line).count();
            int shared = (int) s.shared().stream().filter(p -> p.getLine().equals(line.name())).count();
            return new ProductSummary(line, line.label(), holding(own, shared), own, shared);
        }).toList();
        int protectedCount = (int) products.stream().filter(p -> p.holding() != Holding.NONE).count();
        return new Overview(DISPLAY_ORDER.size(), protectedCount, s.opin(), products);
    }

    public ProductView product(Long customerId, String lineParam, String query) {
        ProductLine line = parseLine(lineParam);
        String q = normalize(query == null ? "" : query.trim());
        if (q.length() > MAX_QUERY_LENGTH) {
            throw ApiException.unprocessable("QUERY_TOO_LONG",
                    "Busca deve ter no máximo " + MAX_QUERY_LENGTH + " caracteres");
        }
        Snapshot s = snapshot(customerId);
        LocalDate today = LocalDate.now(clock);
        List<Item> all = new ArrayList<>();
        s.own().stream().filter(p -> p.getLine() == line).forEach(p -> all.add(new Item(Source.INSUREHUB,
                "InsureHub", p.getNumber(), p.getPlanName(), p.getInsuredName(), p.getStartDate(), p.getEndDate(),
                PolicyDetailsService.forDisplay(line, p.getDetails(), p.getInsuredAmount(),
                        p.getMonthlyContribution(), p.contributionsMade(today)))));
        s.shared().stream().filter(p -> p.getLine().equals(line.name()))
                .sorted(Comparator.comparing(ExternalPolicy::getEndDate))
                .forEach(p -> all.add(new Item(Source.OPIN, p.getInsurer(), p.getPolicyNumber(), p.getPlanName(),
                        p.getInsuredName(), p.getStartDate(), p.getEndDate(), p.getDetails())));
        List<Item> filtered = q.isEmpty() ? all : all.stream().filter(i -> matches(i, q)).toList();
        return new ProductView(line, line.label(), s.opin(), all.size(), filtered);
    }

    private Snapshot snapshot(Long customerId) {
        LocalDate today = LocalDate.now(clock);
        List<Policy> own = policies.findByCustomerIdAndStatusOrderByIdDesc(customerId, Policy.Status.ACTIVE);
        if (consentService.findValid(customerId).isEmpty()) {
            return new Snapshot(new OpinState(false, false, null, false, null), own, List.of());
        }
        try {
            ExternalPolicyService.Result r = externalPolicyService.list(customerId, false);
            List<ExternalPolicy> shared = r.policies().stream()
                    .filter(p -> isKnownLine(p.getLine()) && !p.getEndDate().isBefore(today))
                    .toList();
            return new Snapshot(new OpinState(true, true, r.source(), r.stale(), r.syncedAt()), own, shared);
        } catch (ApiException e) {
            if (e.getStatus() != HttpStatus.SERVICE_UNAVAILABLE) {
                throw e;
            }
            return new Snapshot(new OpinState(true, false, null, false, null), own, List.of());
        }
    }

    private static Holding holding(int own, int shared) {
        if (own > 0 && shared > 0) return Holding.INSUREHUB_OPIN;
        if (own > 0) return Holding.INSUREHUB;
        if (shared > 0) return Holding.OPIN;
        return Holding.NONE;
    }

    private static boolean isKnownLine(String line) {
        return DISPLAY_ORDER.stream().anyMatch(l -> l.name().equals(line));
    }

    private static ProductLine parseLine(String value) {
        return DISPLAY_ORDER.stream()
                .filter(l -> l.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Produto não encontrado"));
    }

    private static boolean matches(Item item, String q) {
        return normalize(item.planName()).contains(q)
                || normalize(item.institution()).contains(q)
                || normalize(item.policyNumber()).contains(q);
    }

    /** Minúsculas e sem acentos: "previdência" encontra "Previdencia". */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
