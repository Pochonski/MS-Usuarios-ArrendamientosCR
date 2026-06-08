package com.arrendamientos.usuarios.infrastructure.web;

import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import com.arrendamientos.usuarios.infrastructure.web.filter.ApimHeaderFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApimHeaderFilterTest {

    private ApimHeaderFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        AppProperties props = buildProps("test-subscription-key", "ABC123", true, List.of("/api/health", "/actuator/**"));
        filter = new ApimHeaderFilter(props, "test", true, new ObjectMapper());
        chain = mock(FilterChain.class);
    }

    private static AppProperties buildProps(String subKey, String thumbprint, boolean validateCert, List<String> skipPaths) {
        return new AppProperties(
                new AppProperties.Jwt(null, null, null, null),
                new AppProperties.Apim(subKey, thumbprint, validateCert, "", skipPaths),
                new AppProperties.Google("", ""),
                new AppProperties.GitHub("", "", null, null),
                new AppProperties.EmailVerification(""),
                new AppProperties.RateLimit(15, 5, 200, 50, 100),
                new AppProperties.Cors(List.of("*")),
                new AppProperties.Lockout(5, 15),
                new AppProperties.TokenRevocation(7),
                new AppProperties.Bcrypt(4),
                new AppProperties.Security(List.of())
        );
    }

    @Test
    void dejaPasarSiFaltaSubscriptionKey() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void dejaPasarSiSubscriptionKeyIncorrecta() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        req.addHeader("Ocp-Apim-Subscription-Key", "wrong");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        assertEquals(403, res.getStatus());
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void dejaPasarSiSubscriptionKeyCorrectaSinCertRequerido() throws ServletException, IOException {
        AppProperties props = buildProps("test-subscription-key", "ABC123", false, List.of("/api/health"));
        ApimHeaderFilter f = new ApimHeaderFilter(props, "test", true, new ObjectMapper());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        req.addHeader("Ocp-Apim-Subscription-Key", "test-subscription-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, chain);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void dejaPasarSiFaltaCertCuandoRequeridoPeroNoEnviado() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        req.addHeader("Ocp-Apim-Subscription-Key", "test-subscription-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        assertEquals(403, res.getStatus());
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void saltaValidacionParaHealth() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void deshabilitadoNoValidaNada() throws ServletException, IOException {
        AppProperties props = buildProps("", "", false, List.of());
        ApimHeaderFilter f = new ApimHeaderFilter(props, "test", false, new ObjectMapper());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, chain);
        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void saltaValidacionEnDevPorDefault() throws ServletException, IOException {
        AppProperties props = buildProps("", "", false, List.of());
        ApimHeaderFilter f = new ApimHeaderFilter(props, "dev", true, new ObjectMapper());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, chain);
        verify(chain, times(1)).doFilter(req, res);
    }
}
