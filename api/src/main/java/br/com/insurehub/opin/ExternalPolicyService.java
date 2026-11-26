package br.com.insurehub.opin;

import br.com.insurehub.common.ApiException;
import br.com.insurehub.config.AppProperties;
import br.com.insurehub.user.User;
import br.com.insurehub.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RN-08: cache fresco → CACHE; senão chama a OPIN → LIVE; OPIN fora → CACHE_FALLBACK (até 24 h) ou 503.
 * A chamada HTTP fica fora de transação para não segurar conexão com o banco durante o timeout.
 */
@Service
public class ExternalPolicyService {

    public enum Source { LIVE, CACHE, CACHE_FALLBACK }

    public record Result(Source source, boolean stale, Instant syncedAt, List<ExternalPolicy> policies) {}

    private final ConsentService consentService;
    private final ConsentRepository consents;
    private final ExternalPolicyRepository cache;
    private final OpinClient opinClient;
    private final UserService userService;
    private final AppProperties props;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ExternalPolicyService(ConsentService consentService, ConsentRepository consents,
                                 ExternalPolicyRepository cache, OpinClient opinClient, UserService userService,
                                 AppProperties props, TransactionTemplate tx, Clock clock) {
        this.consentService = consentService;
        this.consents = consents;
        this.cache = cache;
        this.opinClient = opinClient;
        this.userService = userService;
        this.props = props;
        this.tx = tx;
        this.clock = clock;
    }

    public Result list(Long customerId, boolean refresh) {
        Consent consent = consentService.requireValid(customerId);
        Instant now = clock.instant();
        Instant lastSync = consent.getLastSyncAt();

        boolean fresh = lastSync != null
                && lastSync.isAfter(now.minus(props.opin().freshCacheMinutes(), ChronoUnit.MINUTES));
        if (fresh && !refresh) {
            return new Result(Source.CACHE, false, lastSync, cached(customerId));
        }

        User customer = userService.get(customerId);
        try {
            List<OpinClient.OpinPolicy> live = opinClient.fetchPolicies(customer.getCpf());
            Instant syncedAt = clock.instant();
            List<ExternalPolicy> saved = tx.execute(status -> replaceCache(customerId, consent.getId(), live,
                    syncedAt));
            return new Result(Source.LIVE, false, syncedAt, saved);
        } catch (OpinClient.OpinUnavailableException e) {
            boolean usable = lastSync != null
                    && lastSync.isAfter(now.minus(props.opin().fallbackCacheHours(), ChronoUnit.HOURS));
            if (usable) {
                return new Result(Source.CACHE_FALLBACK, true, lastSync, cached(customerId));
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OPIN_UNAVAILABLE",
                    "Open Insurance indisponível e sem dados em cache");
        }
    }

    private List<ExternalPolicy> cached(Long customerId) {
        return cache.findByCustomerIdOrderByEndDateAsc(customerId);
    }

    private List<ExternalPolicy> replaceCache(Long customerId, Long consentId, List<OpinClient.OpinPolicy> live,
                                              Instant syncedAt) {
        cache.deleteByCustomerId(customerId);
        List<ExternalPolicy> rows = live.stream().map(p -> new ExternalPolicy(customerId, p, syncedAt)).toList();
        cache.saveAll(rows);
        consents.findById(consentId).ifPresent(c -> c.markSynced(syncedAt));
        return cached(customerId);
    }
}
