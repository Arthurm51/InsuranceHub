package br.com.insurehub.opin;

import br.com.insurehub.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opin")
@Tag(name = "Open Insurance")
public class OpinController {

    private final ConsentService consentService;
    private final ExternalPolicyService externalPolicyService;

    public OpinController(ConsentService consentService, ExternalPolicyService externalPolicyService) {
        this.consentService = consentService;
        this.externalPolicyService = externalPolicyService;
    }

    public record ConsentResponse(Long id, Consent.Status status, Instant grantedAt, Instant expiresAt,
                                  Instant revokedAt, Instant lastSyncAt) {
        static ConsentResponse from(Consent c) {
            return new ConsentResponse(c.getId(), c.getStatus(), c.getGrantedAt(), c.getExpiresAt(),
                    c.getRevokedAt(), c.getLastSyncAt());
        }
    }

    public record ExternalPolicyResponse(String externalId, String insurer, String line, String policyNumber,
                                         String planName, String insuredName, BigDecimal insuredAmount,
                                         BigDecimal annualPremium, LocalDate startDate, LocalDate endDate,
                                         Map<String, Object> details) {
        static ExternalPolicyResponse from(ExternalPolicy p) {
            return new ExternalPolicyResponse(p.getExternalId(), p.getInsurer(), p.getLine(), p.getPolicyNumber(),
                    p.getPlanName(), p.getInsuredName(), p.getInsuredAmount(), p.getAnnualPremium(),
                    p.getStartDate(), p.getEndDate(), p.getDetails());
        }
    }

    public record ExternalPoliciesResponse(ExternalPolicyService.Source source, boolean stale, Instant syncedAt,
                                           List<ExternalPolicyResponse> policies) {}

    @PostMapping("/consents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Concede consentimento OPIN por 12 meses (RN-07)")
    public ConsentResponse grant(@AuthenticationPrincipal Jwt jwt) {
        return ConsentResponse.from(consentService.grant(CurrentUser.id(jwt)));
    }

    @GetMapping("/consents/active")
    @Operation(summary = "Consentimento ativo do cliente")
    public ConsentResponse active(@AuthenticationPrincipal Jwt jwt) {
        return ConsentResponse.from(consentService.getActive(CurrentUser.id(jwt)));
    }

    @DeleteMapping("/consents/active")
    @Operation(summary = "Revoga consentimento e apaga cache externo (RN-07)")
    public ConsentResponse revoke(@AuthenticationPrincipal Jwt jwt) {
        return ConsentResponse.from(consentService.revoke(CurrentUser.id(jwt)));
    }

    @GetMapping("/external-policies")
    @Operation(summary = "Apólices de congêneres com cache de contingência (RN-08)")
    public ExternalPoliciesResponse externalPolicies(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(defaultValue = "false") boolean refresh) {
        ExternalPolicyService.Result r = externalPolicyService.list(CurrentUser.id(jwt), refresh);
        return new ExternalPoliciesResponse(r.source(), r.stale(), r.syncedAt(),
                r.policies().stream().map(ExternalPolicyResponse::from).toList());
    }
}
