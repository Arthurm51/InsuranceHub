package br.com.insurehub.portability;

import br.com.insurehub.quote.QuoteDtos.QuoteResponse;
import br.com.insurehub.quote.QuoteService;
import br.com.insurehub.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portability")
@Tag(name = "Portabilidade")
public class PortabilityController {

    private final PortabilityService service;
    private final QuoteService quoteService;

    public PortabilityController(PortabilityService service, QuoteService quoteService) {
        this.service = service;
        this.quoteService = quoteService;
    }

    public record PortabilityRequest(@NotBlank(message = "externalPolicyId é obrigatório") String externalPolicyId) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera cotação de portabilidade com 10% de desconto (RN-09)")
    public QuoteResponse request(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PortabilityRequest req) {
        return QuoteResponse.from(service.requestQuote(CurrentUser.id(jwt), req.externalPolicyId()),
                quoteService.now());
    }
}
