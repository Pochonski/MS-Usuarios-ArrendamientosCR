package com.arrendamientos.usuarios.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.net.URI;
import java.util.Base64;

/**
 * Levanta un WireMock server que responde como Google OAuth
 * (endpoint de descubrimiento + verificación de ID tokens)
 * para tests que requieren verificación real de tokens sin llamar a Google.
 */
public class GoogleOAuthWireMock {

    private final WireMockServer server;
    private final String certsUrl;
    private final String tokenInfoUrl;

    public GoogleOAuthWireMock() {
        this.server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        this.server.start();
        this.certsUrl = "http://localhost:" + server.port() + "/oauth2/v3/certs";
        this.tokenInfoUrl = "http://localhost:" + server.port() + "/oauth2/v3/tokeninfo";
        configureStubs();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public String getCertsUrl() {
        return certsUrl;
    }

    public String getTokenInfoUrl() {
        return tokenInfoUrl;
    }

    public int port() {
        return server.port();
    }

    public void stop() {
        server.stop();
    }

    private void configureStubs() {
        // Stub del JWKS endpoint (necesario para GoogleIdTokenVerifier)
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
                com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/oauth2/v3/certs"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"keys\":[]}")));

        // Stub del tokeninfo endpoint (alternativa simple para tests)
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/oauth2/v3/tokeninfo"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
    }

    /**
     * Genera un ID token JWT firmado con HS256 (compatible con GoogleIdTokenVerifier si se
     * configura correctamente). En la práctica, para tests de integración con WireMock se
     * usa el flujo de tokeninfo.
     */
    public String fakeIdToken(String email, String name, String googleId, String audience) {
        long now = System.currentTimeMillis() / 1000;
        String header = base64Url("{\"alg\":\"RS256\",\"kid\":\"test-key\",\"typ\":\"JWT\"}");
        String payload = base64Url(String.format(
                "{\"iss\":\"https://accounts.google.com\",\"aud\":\"%s\",\"sub\":\"%s\","
                        + "\"email\":\"%s\",\"name\":\"%s\",\"email_verified\":true,"
                        + "\"iat\":%d,\"exp\":%d}",
                audience, googleId, email, name, now, now + 3600
        ));
        // Firma dummy — GoogleIdTokenVerifier.validate() rechazaría esto, pero tokeninfo sí valida
        String signature = base64Url("fake-signature");
        return header + "." + payload + "." + signature;
    }

    private static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes());
    }

    public URI baseUri() {
        return URI.create("http://localhost:" + server.port());
    }
}
