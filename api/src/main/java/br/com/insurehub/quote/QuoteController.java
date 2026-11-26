package br.com.insurehub.quote;

import br.com.insurehub.quote.QuoteDtos.QuoteRequest;
import br.com.insurehub.quote.QuoteDtos.QuoteResponse;
import br.com.insurehub.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quotes")
@Tag(name = "Cotações")
public class QuoteController {

    private final QuoteService service;

    public QuoteController(QuoteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria cotação (RN-03)")
    public QuoteResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuoteRequest req) {
        Quote quote = service.create(CurrentUser.id(jwt), req);
        return QuoteResponse.from(quote, service.now());
    }

    @GetMapping
    @Operation(summary = "Lista cotações do cliente")
    public List<QuoteResponse> list(@AuthenticationPrincipal Jwt jwt) {
        Instant now = service.now();
        return service.list(CurrentUser.id(jwt)).stream().map(q -> QuoteResponse.from(q, now)).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha cotação do cliente")
    public QuoteResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return QuoteResponse.from(service.get(CurrentUser.id(jwt), id), service.now());
    }
}
