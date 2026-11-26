package br.com.insurehub.policy.details;

import br.com.insurehub.common.ApiError.FieldViolation;
import br.com.insurehub.common.Money;
import br.com.insurehub.common.ValidationException;
import br.com.insurehub.quote.ProductLine;
import br.com.insurehub.quote.Quote;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Valida os dados do produto informados na emissão e devolve o JSON a ser gravado em policies.details. */
@Service
public class PolicyDetailsService {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final ObjectMapper mapper;
    private final Validator validator;

    public PolicyDetailsService(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public Map<String, Object> validate(Quote quote, JsonNode raw) {
        if (raw == null || raw.isNull() || !raw.isObject()) {
            throw ValidationException.of("details", "Dados do produto são obrigatórios");
        }
        ProductLine line = quote.getLine();
        Map<String, Object> stored = switch (line) {
            case AUTO -> {
                ProductDetails.Auto d = parse(raw, ProductDetails.Auto.class);
                requireUniqueDriverCpfs(d);
                Map<String, Object> m = toMap(d);
                m.put("plate", d.plate().replace("-", "").toUpperCase(Locale.ROOT));
                m.put("deductible", Money.round(quote.getInsuredAmount().multiply(quote.getPlan().autoDeductibleRate())));
                yield m;
            }
            case VIDA -> {
                ProductDetails.Vida d = parse(raw, ProductDetails.Vida.class);
                requireHundredPercent(d.beneficiaries());
                yield toMap(d);
            }
            case PREVIDENCIA -> {
                ProductDetails.Previdencia d = parse(raw, ProductDetails.Previdencia.class);
                requireHundredPercent(d.beneficiaries());
                Map<String, Object> m = toMap(d);
                m.put("adminFee", quote.getPlan().pensionAdminFee());
                yield m;
            }
            case VIAGEM -> {
                Map<String, Object> m = toMap(parse(raw, ProductDetails.Viagem.class));
                m.put("tripDays", quote.getTripDays());
                yield m;
            }
            case RESIDENCIAL -> toMap(parse(raw, ProductDetails.Residencial.class));
        };
        return stored;
    }

    private <T> T parse(JsonNode raw, Class<T> type) {
        T value;
        try {
            value = mapper.treeToValue(raw, type);
        } catch (Exception e) {
            throw ValidationException.of("details", "Formato inválido para os dados do produto");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            List<FieldViolation> fields = violations.stream()
                    .map(v -> new FieldViolation("details." + cleanPath(v.getPropertyPath().toString()), v.getMessage()))
                    .sorted(Comparator.comparing(FieldViolation::field))
                    .toList();
            throw new ValidationException(fields);
        }
        return value;
    }

    /** "drivers[0].<list element>.cpf" → "drivers[0].cpf" */
    private static String cleanPath(String path) {
        return path.replace(".<list element>", "");
    }

    private void requireUniqueDriverCpfs(ProductDetails.Auto d) {
        Set<String> seen = new HashSet<>();
        List<FieldViolation> errors = new ArrayList<>();
        for (int i = 0; i < d.drivers().size(); i++) {
            if (!seen.add(d.drivers().get(i).cpf())) {
                errors.add(new FieldViolation("details.drivers[" + i + "].cpf", "CPF de condutor repetido"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private void requireHundredPercent(List<ProductDetails.Beneficiary> beneficiaries) {
        int total = beneficiaries.stream().mapToInt(ProductDetails.Beneficiary::percentage).sum();
        if (total != 100) {
            throw ValidationException.of("details.beneficiaries",
                    "A soma dos percentuais dos beneficiários deve ser 100 (atual: " + total + ")");
        }
    }

    private Map<String, Object> toMap(Object record) {
        return new LinkedHashMap<>(mapper.convertValue(record, MAP));
    }

    /** Campos calculados na leitura, para exibição (tela de produto). */
    public static Map<String, Object> forDisplay(ProductLine line, Map<String, Object> stored, BigDecimal insuredAmount,
                                                 BigDecimal monthlyContribution, long contributionsMade) {
        Map<String, Object> m = new LinkedHashMap<>(stored);
        switch (line) {
            case VIDA -> m.put("insuredCapital", insuredAmount);
            case RESIDENCIAL -> m.put("coverageAmount", insuredAmount);
            case PREVIDENCIA -> {
                m.put("monthlyContribution", monthlyContribution);
                m.put("accumulatedBalance",
                        Money.round(monthlyContribution.multiply(BigDecimal.valueOf(contributionsMade))));
            }
            default -> { }
        }
        return m;
    }
}
