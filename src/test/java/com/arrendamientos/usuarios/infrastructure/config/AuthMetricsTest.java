package com.arrendamientos.usuarios.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthMetricsTest {

    private MeterRegistry registry;
    private AuthMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AuthMetrics(registry);
    }

    @Test
    void loginSuccessIncrementa() {
        assertEquals(0.0, registry.find("auth.login.success").counter().count());
        metrics.loginSuccess();
        metrics.loginSuccess();
        assertEquals(2.0, registry.find("auth.login.success").counter().count());
    }

    @Test
    void loginFailureIncrementa() {
        metrics.loginFailure();
        assertEquals(1.0, registry.find("auth.login.failure").counter().count());
    }

    @Test
    void registerSuccessYConflict() {
        metrics.registerSuccess();
        metrics.registerConflict();
        assertEquals(1.0, registry.find("auth.register.success").counter().count());
        assertEquals(1.0, registry.find("auth.register.conflict").counter().count());
    }

    @Test
    void googleSuccessYFailure() {
        metrics.googleSuccess();
        metrics.googleFailure();
        assertEquals(1.0, registry.find("auth.google.success").counter().count());
        assertEquals(1.0, registry.find("auth.google.failure").counter().count());
    }

    @Test
    void accountLockedIncrementa() {
        metrics.accountLocked();
        assertEquals(1.0, registry.find("auth.account.locked").counter().count());
    }

    @Test
    void tokenRefreshIncrementa() {
        metrics.tokenRefresh();
        assertEquals(1.0, registry.find("auth.token.refresh").counter().count());
    }

    @Test
    void logoutIncrementa() {
        metrics.logout();
        assertEquals(1.0, registry.find("auth.logout").counter().count());
    }

    @Test
    void todosLosCountersEstanRegistrados() {
        String[] expectedCounters = {
                "auth.login.success", "auth.login.failure",
                "auth.register.success", "auth.register.conflict",
                "auth.google.success", "auth.google.failure",
                "auth.account.locked", "auth.token.refresh", "auth.logout"
        };
        for (String name : expectedCounters) {
            assertNotNull(registry.find(name).counter(), "Counter faltante: " + name);
        }
    }
}
