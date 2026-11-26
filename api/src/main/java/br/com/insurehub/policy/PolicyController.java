package br.com.insurehub.policy;

import br.com.insurehub.policy.PolicyDtos.CancelRequest;
import br.com.insurehub.policy.PolicyDtos.IssueRequest;
import br.com.insurehub.policy.PolicyDtos.PolicyResponse;
import br.com.insurehub.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
@Tag(name = "Apólices")
public class PolicyController {

    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Emite apólice a partir de cotação, com os dados do produto (RN-04)")
    public PolicyResponse issue(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody IssueRequest req) {
        return PolicyResponse.from(
                service.issue(CurrentUser.id(jwt), req.quoteId(), req.startDate(), req.details()), service.today());
    }

    @GetMapping
    @Operation(summary = "Lista apólices do cliente, com filtro opcional por status")
    public List<PolicyResponse> list(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) Policy.Status status) {
        LocalDate today = service.today();
        return service.list(CurrentUser.id(jwt), status).stream().map(p -> PolicyResponse.from(p, today)).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha apólice do cliente")
    public PolicyResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return PolicyResponse.from(service.get(CurrentUser.id(jwt), id), service.today());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela apólice (RN-05)")
    public PolicyResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @Valid @RequestBody CancelRequest req) {
        return PolicyResponse.from(service.cancel(CurrentUser.id(jwt), id, req.reason()), service.today());
    }
}
