package com.taskflow.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe view of the "app.*" block in application.yml.
 * Prefer this over scattering @Value("${...}") around the code base.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Mail mail) {

    public record Jwt(String secret, long expiryMinutes) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Mail(boolean enabled, String from) {}
}
