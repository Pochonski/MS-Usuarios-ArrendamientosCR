package com.arrendamientos.usuarios.infrastructure.github;

import com.arrendamientos.usuarios.domain.model.GitHubUserInfo;
import com.arrendamientos.usuarios.domain.port.out.GitHubTokenVerifierPort;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;

/**
 * Adapter que intercambia un `code` de GitHub OAuth por un `access_token` y
 * luego obtiene el perfil del usuario en `https://api.github.com/user`.
 *
 * Flujo:
 *  1) POST https://github.com/login/oauth/access_token (form-urlencoded)
 *     → { access_token, scope, token_type }
 *  2) GET https://api.github.com/user (Authorization: Bearer ...)
 *     → { id, login, name, email, avatar_url }
 *
 * Notas:
 *  - El client_secret NUNCA sale del backend.
 *  - GitHub puede devolver email=null si el usuario lo oculta en su perfil;
 *    en ese caso usamos `login` como display name.
 *  - No validamos `email_verified` (GitHub no expone este campo) — flexible
 *    según requerimiento del producto.
 */
@Component
public class GitHubTokenVerifierAdapter implements GitHubTokenVerifierPort {

    private static final Logger log = LoggerFactory.getLogger(GitHubTokenVerifierAdapter.class);

    private static final String USER_AGENT = "ms-usuarios-boot";
    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GitHubTokenVerifierAdapter(AppProperties properties,
                                      RestClient.Builder restClientBuilder,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public GitHubUserInfo verificar(String code, String redirectUri) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code de GitHub es requerido");
        }
        com.arrendamientos.usuarios.infrastructure.config.AppProperties.GitHub gh = properties.gitHub();
        if (gh == null
                || gh.clientId() == null || gh.clientId().isBlank()
                || gh.clientSecret() == null || gh.clientSecret().isBlank()) {
            throw new IllegalStateException(
                    "GitHub OAuth not configured: app.github.client-id/client-secret missing or empty "
                  + "(check GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET env vars on the App Service)");
        }

        String accessToken = exchangeCodeForToken(code, redirectUri);
        return fetchUserProfile(accessToken);
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.gitHub().clientId());
        form.add("client_secret", properties.gitHub().clientSecret());
        form.add("code", code);
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add("redirect_uri", redirectUri);
        }

        try {
            String body = restClient.post()
                    .uri(URI.create(properties.gitHub().tokenUrl()))
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("GitHub token endpoint devolvió respuesta vacía");
            }

            JsonNode json = objectMapper.readTree(body);

            if (json.has("error")) {
                String error = json.path("error").asText("unknown");
                String description = json.path("error_description").asText("");
                log.warn("GitHub OAuth error: {} - {}", error, description);
                throw new IllegalArgumentException("Code de GitHub inválido: " + error);
            }

            String accessToken = json.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("GitHub no devolvió access_token");
            }
            return accessToken;
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            log.warn("GitHub token endpoint respondió {}: {}", status, e.getResponseBodyAsString());
            throw new IllegalArgumentException("Error intercambiando code con GitHub: HTTP " + status.value());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado llamando a GitHub token endpoint", e);
            throw new IllegalArgumentException("Error intercambiando code con GitHub", e);
        }
    }

    private GitHubUserInfo fetchUserProfile(String accessToken) {
        try {
            String body = restClient.get()
                    .uri(URI.create(properties.gitHub().userUrl()))
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("GitHub /user devolvió respuesta vacía");
            }

            JsonNode json = objectMapper.readTree(body);

            JsonNode idNode = json.get("id");
            if (idNode == null || idNode.isNull()) {
                throw new IllegalArgumentException("GitHub no devolvió id de usuario");
            }
            long githubId = idNode.asLong();

            String login = json.path("login").asText(null);
            String name = json.path("name").asText(null);
            String email = json.path("email").asText(null);
            String avatar = json.path("avatar_url").asText(null);

            if ((name == null || name.isBlank()) && login != null) {
                name = login;
            }
            if (name == null || name.isBlank()) {
                name = "GitHub User";
            }

            if (email != null) {
                email = email.trim().toLowerCase();
            }

            return new GitHubUserInfo(githubId, login, email, name, avatar);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            log.warn("GitHub /user respondió {}: {}", status, e.getResponseBodyAsString());
            throw new IllegalArgumentException("Error obteniendo perfil de GitHub: HTTP " + status.value());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado llamando a GitHub /user", e);
            throw new IllegalArgumentException("Error obteniendo perfil de GitHub", e);
        }
    }
}
