package com.arrendamientos.usuarios.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Apim apim,
        Google google,
        GitHub gitHub,
        EmailVerification emailVerification,
        RateLimit rateLimit,
        Cors cors,
        Lockout lockout,
        TokenRevocation tokenRevocation,
        Bcrypt bcrypt,
        Security security
) {

    public record Jwt(
            @NotBlank String secret,
            Duration expiresIn,
            Duration refreshExpiresIn,
            Duration emailVerificationExpiresIn
    ) {}

    public record Apim(
            String subscriptionKey,
            String clientCertThumbprint,
            boolean validateClientCert,
            String internalApiUrl,
            List<String> skipPaths
    ) {}

    public record Google(
            String clientId,
            String allowedDomain
    ) {}

    public record GitHub(
            String clientId,
            String clientSecret,
            String tokenUrl,
            String userUrl
    ) {
        public GitHub {
            // Compact constructor: aplica defaults si Spring no bindea los URLs.
            // Antes había un constructor de 2-args que Spring Boot 3 con @ConfigurationProperties
            // sobre records NO usaba para binding — bindeaba el constructor canónico de 4
            // args, pero como los URLs venían null, el binding entero fallaba silenciosamente
            // y `properties.gitHub()` quedaba null. Por eso el IllegalStateException con NPE.
            if (tokenUrl == null || tokenUrl.isBlank()) {
                tokenUrl = "https://github.com/login/oauth/access_token";
            }
            if (userUrl == null || userUrl.isBlank()) {
                userUrl = "https://api.github.com/user";
            }
        }
    }

    public record EmailVerification(
            String frontendBaseUrl
    ) {}

    public record RateLimit(
            @Positive int windowMinutes,
            @Positive int authMax,
            @Positive int readMax,
            @Positive int writeMax,
            @Positive int generalMax
    ) {}

    public record Cors(
            @NotEmpty List<String> allowedOrigins
    ) {}

    public record Lockout(
            @Positive int maxAttempts,
            @Positive int durationMinutes
    ) {}

    public record TokenRevocation(
            @Positive int cleanupAfterDays
    ) {}

    public record Bcrypt(
            @Positive int strength
    ) {}

    public record Security(
            @NotEmpty List<String> endpointsPublic
    ) {}
}
