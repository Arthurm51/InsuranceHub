package br.com.insurehub.auth;

import br.com.insurehub.auth.AuthDtos.LoginRequest;
import br.com.insurehub.auth.AuthDtos.RegisterRequest;
import br.com.insurehub.auth.AuthDtos.TokenResponse;
import br.com.insurehub.security.CurrentUser;
import br.com.insurehub.user.UserResponse;
import br.com.insurehub.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra cliente (RN-01)")
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        return UserResponse.from(authService.register(req));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Autentica e retorna JWT (RN-02)")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.get(CurrentUser.id(jwt)));
    }
}
