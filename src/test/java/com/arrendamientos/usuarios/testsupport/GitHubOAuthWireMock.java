package com.arrendamientos.usuarios.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.net.URI;

/**
 * Levanta un WireMock server que simula los endpoints de GitHub OAuth
 * (token exchange + /user) para tests que requieren verificación real
 * sin llamar a github.com.
 */
public class GitHubOAuthWireMock {

    private final WireMockServer server;

    public GitHubOAuthWireMock() {
        this.server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        this.server.start();
        configureStubs();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public int port() {
        return server.port();
    }

    public String tokenUrl() {
        return "http://localhost:" + server.port() + "/login/oauth/access_token";
    }

    public String userUrl() {
        return "http://localhost:" + server.port() + "/user";
    }

    public URI baseUri() {
        return URI.create("http://localhost:" + server.port());
    }

    public void stop() {
        server.stop();
    }

    private void configureStubs() {
        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/login/oauth/access_token"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"fake-access-token\",\"scope\":\"read:user,user:email\",\"token_type\":\"bearer\"}")));

        server.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
                com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/user"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":12345,\"login\":\"octocat\",\"name\":\"Octo Cat\",\"email\":\"octo@example.com\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/12345\"}")));
    }
}
