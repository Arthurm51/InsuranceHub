package br.com.insurehub.config;

import br.com.insurehub.opin.ConsentService;
import br.com.insurehub.policy.PolicyService;
import br.com.insurehub.quote.Plan;
import br.com.insurehub.quote.ProductLine;
import br.com.insurehub.quote.Quote;
import br.com.insurehub.quote.QuoteDtos.QuoteRequest;
import br.com.insurehub.quote.QuoteService;
import br.com.insurehub.user.Role;
import br.com.insurehub.user.User;
import br.com.insurehub.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga inicial de usuários de demonstração (ver README > Massa de dados).
 * Só roda com insurehub.seed.enabled=true e banco sem usuários.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final QuoteService quoteService;
    private final PolicyService policyService;
    private final ConsentService consentService;
    private final ObjectMapper mapper;
    private final AppProperties props;
    private final Clock clock;

    public DataSeeder(UserRepository users, PasswordEncoder encoder, QuoteService quoteService,
                      PolicyService policyService, ConsentService consentService, ObjectMapper mapper,
                      AppProperties props, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.quoteService = quoteService;
        this.policyService = policyService;
        this.consentService = consentService;
        this.mapper = mapper;
        this.props = props;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seed().enabled() || users.count() > 0) {
            return;
        }
        String customerPassword = encoder.encode("Senha123");
        users.save(new User("Analista InsureHub", "12345678909", "admin@insurehub.com",
                encoder.encode("Admin123"), LocalDate.of(1985, 1, 15), Role.ADMIN, clock.instant()));
        users.save(new User("Maria Oliveira", "52998224725", "maria@insurehub.com", customerPassword,
                LocalDate.of(1990, 5, 10), Role.CUSTOMER, clock.instant()));
        users.save(new User("João Pereira", "39053344705", "joao@insurehub.com", customerPassword,
                LocalDate.of(2003, 3, 15), Role.CUSTOMER, clock.instant()));
        users.save(new User("Ana Souza", "71460238001", "ana@insurehub.com", customerPassword,
                LocalDate.of(1970, 8, 20), Role.CUSTOMER, clock.instant()));
        User demo = users.save(new User("Paula Andrade", "98765432100", "demo@insurehub.com", customerPassword,
                LocalDate.of(1988, 7, 3), Role.CUSTOMER, clock.instant()));
        seedDemoPortfolio(demo);
        log.info("Massa inicial criada: 1 admin e 4 clientes (demo com carteira preenchida)");
    }

    /** Paula: Auto contratado na InsureHub + consentimento OPIN (Auto, Vida, Previdência e Viagem compartilhados). */
    private void seedDemoPortfolio(User demo) {
        Quote quote = quoteService.create(demo.getId(),
                new QuoteRequest(ProductLine.AUTO, Plan.COMPLETO, new BigDecimal("92000.00"), null, null));
        Map<String, Object> details = Map.of(
                "plate", "BRA2E19",
                "drivers", List.of(
                        Map.of("name", "Paula Andrade", "cpf", "98765432100"),
                        Map.of("name", "Rafael Andrade", "cpf", "52998224725")));
        policyService.issue(demo.getId(), quote.getId(), null, mapper.valueToTree(details));
        consentService.grant(demo.getId());
    }
}
