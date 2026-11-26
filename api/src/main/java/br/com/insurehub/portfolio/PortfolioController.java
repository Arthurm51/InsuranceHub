package br.com.insurehub.portfolio;

import br.com.insurehub.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
@Tag(name = "Carteira")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Resumo da carteira: produtos protegidos de 5 (RN-11)")
    public PortfolioService.Overview overview(@AuthenticationPrincipal Jwt jwt) {
        return service.overview(CurrentUser.id(jwt));
    }

    @GetMapping("/{line}")
    @Operation(summary = "Apólices de um produto, com busca por plano, instituição ou número (RN-12)")
    public PortfolioService.ProductView product(@AuthenticationPrincipal Jwt jwt, @PathVariable String line,
                                                @RequestParam(required = false) String q) {
        return service.product(CurrentUser.id(jwt), line, q);
    }
}
