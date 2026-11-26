package br.com.insurehub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "insurehub")
public record AppProperties(Jwt jwt, Security security, Opin opin, Seed seed) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Security(int maxFailedLogins, long lockMinutes) {}

    public record Opin(String baseUrl, int timeoutMs, long freshCacheMinutes, long fallbackCacheHours) {}

    public record Seed(boolean enabled) {}
}
