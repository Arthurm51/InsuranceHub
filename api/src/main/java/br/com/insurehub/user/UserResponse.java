package br.com.insurehub.user;

import java.time.Instant;
import java.time.LocalDate;

public record UserResponse(Long id, String name, String cpf, String email, LocalDate birthDate, Role role,
                           Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getCpf(), u.getEmail(), u.getBirthDate(), u.getRole(),
                u.getCreatedAt());
    }
}
