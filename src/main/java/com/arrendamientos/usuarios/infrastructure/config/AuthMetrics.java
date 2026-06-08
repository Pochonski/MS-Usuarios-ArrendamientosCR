package com.arrendamientos.usuarios.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métricas de negocio expuestas vía Micrometer y publicadas
 * a Prometheus / Application Insights.
 *
 * Counter names:
 *   - auth.login.success
 *   - auth.login.failure
 *   - auth.register.success
 *   - auth.register.conflict
 *   - auth.google.success
 *   - auth.google.failure
 *   - auth.github.success
 *   - auth.github.failure
 *   - auth.account.locked
 *   - auth.token.refresh
 *   - auth.logout
 */
@Component
public class AuthMetrics {

    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter registerSuccess;
    private final Counter registerConflict;
    private final Counter googleSuccess;
    private final Counter googleFailure;
    private final Counter githubSuccess;
    private final Counter githubFailure;
    private final Counter accountLocked;
    private final Counter tokenRefresh;
    private final Counter logout;

    public AuthMetrics(MeterRegistry registry) {
        this.loginSuccess = Counter.builder("auth.login.success")
                .description("Successful email/password logins")
                .register(registry);
        this.loginFailure = Counter.builder("auth.login.failure")
                .description("Failed email/password logins")
                .register(registry);
        this.registerSuccess = Counter.builder("auth.register.success")
                .description("Successful user registrations")
                .register(registry);
        this.registerConflict = Counter.builder("auth.register.conflict")
                .description("Registration attempts with duplicate email")
                .register(registry);
        this.googleSuccess = Counter.builder("auth.google.success")
                .description("Successful Google OAuth logins")
                .register(registry);
        this.googleFailure = Counter.builder("auth.google.failure")
                .description("Failed Google OAuth attempts")
                .register(registry);
        this.githubSuccess = Counter.builder("auth.github.success")
                .description("Successful GitHub OAuth logins")
                .register(registry);
        this.githubFailure = Counter.builder("auth.github.failure")
                .description("Failed GitHub OAuth attempts")
                .register(registry);
        this.accountLocked = Counter.builder("auth.account.locked")
                .description("Account lockouts triggered by failed attempts")
                .register(registry);
        this.tokenRefresh = Counter.builder("auth.token.refresh")
                .description("Token refresh operations")
                .register(registry);
        this.logout = Counter.builder("auth.logout")
                .description("Logout operations (token revocation)")
                .register(registry);
    }

    public void loginSuccess() { loginSuccess.increment(); }
    public void loginFailure() { loginFailure.increment(); }
    public void registerSuccess() { registerSuccess.increment(); }
    public void registerConflict() { registerConflict.increment(); }
    public void googleSuccess() { googleSuccess.increment(); }
    public void googleFailure() { googleFailure.increment(); }
    public void githubSuccess() { githubSuccess.increment(); }
    public void githubFailure() { githubFailure.increment(); }
    public void accountLocked() { accountLocked.increment(); }
    public void tokenRefresh() { tokenRefresh.increment(); }
    public void logout() { logout.increment(); }
}
