package br.com.insurehub.claim;

import br.com.insurehub.claim.ClaimDtos.ClaimResponse;
import br.com.insurehub.claim.ClaimDtos.OpenClaimRequest;
import br.com.insurehub.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/claims")
@Tag(name = "Sinistros (cliente)")
public class ClaimController {

    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre sinistro (RN-06)")
    public ClaimResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpenClaimRequest req) {
        return ClaimResponse.from(service.open(CurrentUser.id(jwt), req));
    }

    @GetMapping
    @Operation(summary = "Lista sinistros do cliente")
    public List<ClaimResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.listForCustomer(CurrentUser.id(jwt)).stream().map(ClaimResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha sinistro do cliente")
    public ClaimResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ClaimResponse.from(service.getForCustomer(CurrentUser.id(jwt), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cliente cancela sinistro OPEN (RN-06)")
    public ClaimResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ClaimResponse.from(service.cancelByCustomer(CurrentUser.id(jwt), id));
    }
}
