package br.com.insurehub.auth;

import br.com.insurehub.common.Cpf;
import br.com.insurehub.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Nome é obrigatório")
            @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
            String name,

            @NotBlank(message = "CPF é obrigatório")
            @Cpf
            String cpf,

            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail inválido")
            @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres")
            String email,

            @NotBlank(message = "Senha é obrigatória")
            @Size(min = 8, max = 64, message = "Senha deve ter entre 8 e 64 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Senha deve conter ao menos 1 letra e 1 número")
            String password,

            @NotNull(message = "Data de nascimento é obrigatória")
            @Past(message = "Data de nascimento deve estar no passado")
            LocalDate birthDate) {}

    public record LoginRequest(
            @NotBlank(message = "E-mail é obrigatório") String email,
            @NotBlank(message = "Senha é obrigatória") String password) {}

    public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, Role role, String name) {}
}
