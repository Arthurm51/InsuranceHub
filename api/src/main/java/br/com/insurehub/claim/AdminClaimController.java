package br.com.insurehub.claim;

import br.com.insurehub.claim.ClaimDtos.ApproveRequest;
import br.com.insurehub.claim.ClaimDtos.ClaimResponse;
import br.com.insurehub.claim.ClaimDtos.RejectRequest;
import br.com.insurehub.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/claims")
@Tag(name = "Admin - Sinistros")
public class AdminClaimController {

    private final ClaimService service;

    public AdminClaimController(ClaimService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos os sinistros, com filtro opcional por status")
    public List<ClaimResponse> list(@RequestParam(required = false) ClaimStatus status) {
        return service.listAll(status).stream().map(ClaimResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ClaimResponse get(@PathVariable Long id) {
        return ClaimResponse.from(service.get(id));
    }

    @PostMapping("/{id}/start-analysis")
    @Operation(summary = "OPEN -> IN_ANALYSIS")
    public ClaimResponse startAnalysis(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ClaimResponse.from(service.startAnalysis(CurrentUser.id(jwt), id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "IN_ANALYSIS -> APPROVED")
    public ClaimResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                 @Valid @RequestBody ApproveRequest req) {
        return ClaimResponse.from(service.approve(CurrentUser.id(jwt), id, req.approvedAmount()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "IN_ANALYSIS -> REJECTED")
    public ClaimResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody RejectRequest req) {
        return ClaimResponse.from(service.reject(CurrentUser.id(jwt), id, req.reason()));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "APPROVED -> PAID")
    public ClaimResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ClaimResponse.from(service.pay(CurrentUser.id(jwt), id));
    }
}
