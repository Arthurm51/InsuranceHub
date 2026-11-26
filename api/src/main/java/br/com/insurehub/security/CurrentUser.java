package br.com.insurehub.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

/** Extrai o id do usuário autenticado a partir do "sub" do JWT. */
public final class CurrentUser {

    private CurrentUser() {}

    public static Long id(@AuthenticationPrincipal Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
