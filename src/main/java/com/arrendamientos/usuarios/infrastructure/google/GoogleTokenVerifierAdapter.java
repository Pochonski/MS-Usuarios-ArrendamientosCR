package com.arrendamientos.usuarios.infrastructure.google;

import com.arrendamientos.usuarios.domain.model.GoogleUserInfo;
import com.arrendamientos.usuarios.domain.port.out.GoogleTokenVerifierPort;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class GoogleTokenVerifierAdapter implements GoogleTokenVerifierPort {

    private static final Set<String> VALID_ISSUERS = Set.of(
            "accounts.google.com",
            "https://accounts.google.com"
    );

    private final AppProperties properties;
    private GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierAdapter(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() throws Exception {
        if (properties.google().clientId() == null || properties.google().clientId().isBlank()) {
            return;
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(properties.google().clientId()))
                .build();
    }

    @Override
    public GoogleUserInfo verificar(String idToken, String nonce, String hostedDomain) {
        if (verifier == null) {
            throw new IllegalStateException("Google OAuth not configured");
        }
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new IllegalArgumentException("Token de Google inválido");
            }
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            if (payload.getEmail() == null) {
                throw new IllegalArgumentException("Email not provided by Google");
            }
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new IllegalArgumentException("Google account email is not verified");
            }
            if (payload.getIssuer() == null || !VALID_ISSUERS.contains(payload.getIssuer())) {
                throw new IllegalArgumentException("Invalid Google token issuer");
            }
            if (hostedDomain != null && !hostedDomain.isBlank()) {
                if (!hostedDomain.equals(payload.getHostedDomain())) {
                    throw new IllegalArgumentException("Cuenta de Google debe pertenecer al dominio " + hostedDomain);
                }
            }
            if (nonce != null && !nonce.isBlank()) {
                if (!nonce.equals(payload.getNonce())) {
                    throw new IllegalArgumentException("Nonce inválido — posible ataque de replay");
                }
            }
            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    payload.get("name") == null ? "Unknown" : payload.get("name").toString(),
                    payload.get("picture") == null ? null : payload.get("picture").toString()
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Token de Google inválido", e);
        }
    }
}
