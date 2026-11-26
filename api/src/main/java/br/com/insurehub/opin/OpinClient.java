package br.com.insurehub.opin;

import br.com.insurehub.config.AppProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP da API Open Insurance (simulada pelo WireMock em opin-mock/). */
@Component
public class OpinClient {

    private static final Logger log = LoggerFactory.getLogger(OpinClient.class);

    /** insuredAmount é nulo em PREVIDENCIA. details segue o mesmo formato de exibição da InsureHub. */
    public record OpinPolicy(String policyId, String insurer, String line, String policyNumber, String planName,
                             String insuredName, BigDecimal insuredAmount, BigDecimal annualPremium,
                             LocalDate startDate, LocalDate endDate, Map<String, Object> details) {}

    public record OpinResponse(String cpf, List<OpinPolicy> data) {}

    public static class OpinUnavailableException extends RuntimeException {
        public OpinUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final RestClient client;

    public OpinClient(AppProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.opin().timeoutMs());
        factory.setReadTimeout(props.opin().timeoutMs());
        this.client = builder.baseUrl(props.opin().baseUrl()).requestFactory(factory).build();
    }

    public List<OpinPolicy> fetchPolicies(String cpf) {
        try {
            OpinResponse response = client.get()
                    .uri(uri -> uri.path("/opin/v1/insurance-policies").queryParam("cpf", cpf).build())
                    .retrieve()
                    .body(OpinResponse.class);
            if (response == null || response.data() == null) {
                throw new OpinUnavailableException("Resposta vazia da OPIN", null);
            }
            return response.data();
        } catch (RestClientException e) {
            log.warn("OPIN indisponível: {}", e.getMessage());
            throw new OpinUnavailableException("OPIN indisponível", e);
        }
    }
}
