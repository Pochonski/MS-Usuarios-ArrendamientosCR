package com.arrendamientos.usuarios.infrastructure.web;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import com.arrendamientos.usuarios.application.dto.CreateUsuarioCommand;
import com.arrendamientos.usuarios.application.dto.GitHubLoginCommand;
import com.arrendamientos.usuarios.application.dto.GoogleLoginCommand;
import com.arrendamientos.usuarios.application.dto.LoginCommand;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.in.EnviarVerificacionEmailUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginGitHubUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginGoogleUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginUseCase;
import com.arrendamientos.usuarios.domain.port.in.LogoutUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerPerfilUseCase;
import com.arrendamientos.usuarios.domain.port.in.RefreshTokenUseCase;
import com.arrendamientos.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.VerificarEmailUseCase;
import com.arrendamientos.usuarios.domain.port.out.TokenProviderPort;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import com.arrendamientos.usuarios.infrastructure.security.AuthenticatedUser;
import com.arrendamientos.usuarios.infrastructure.web.dto.GitHubLoginRequest;
import com.arrendamientos.usuarios.infrastructure.web.dto.GoogleLoginRequest;
import com.arrendamientos.usuarios.infrastructure.web.dto.LoginRequest;
import com.arrendamientos.usuarios.infrastructure.web.dto.LoginResponseDto;
import com.arrendamientos.usuarios.infrastructure.web.dto.RegistroRequest;
import com.arrendamientos.usuarios.infrastructure.web.dto.UsuarioResponseDto;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Autenticación (login, registro, Google OAuth, perfil)")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LoginGoogleUseCase loginGoogleUseCase;
    private final LoginGitHubUseCase loginGitHubUseCase;
    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final ObtenerPerfilUseCase obtenerPerfilUseCase;
    private final LogoutUseCase logoutUseCase;
    private final VerificarEmailUseCase verificarEmailUseCase;
    private final EnviarVerificacionEmailUseCase enviarVerificacionEmailUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final TokenProviderPort tokenProvider;
    private final AppProperties properties;

    public AuthController(
            LoginUseCase loginUseCase,
            LoginGoogleUseCase loginGoogleUseCase,
            LoginGitHubUseCase loginGitHubUseCase,
            RegistrarUsuarioUseCase registrarUsuarioUseCase,
            ObtenerPerfilUseCase obtenerPerfilUseCase,
            LogoutUseCase logoutUseCase,
            VerificarEmailUseCase verificarEmailUseCase,
            EnviarVerificacionEmailUseCase enviarVerificacionEmailUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            TokenProviderPort tokenProvider,
            AppProperties properties) {
        this.loginUseCase = loginUseCase;
        this.loginGoogleUseCase = loginGoogleUseCase;
        this.loginGitHubUseCase = loginGitHubUseCase;
        this.registrarUsuarioUseCase = registrarUsuarioUseCase;
        this.obtenerPerfilUseCase = obtenerPerfilUseCase;
        this.logoutUseCase = logoutUseCase;
        this.verificarEmailUseCase = verificarEmailUseCase;
        this.enviarVerificacionEmailUseCase = enviarVerificacionEmailUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con email y contraseña")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequest req) {
        AuthResult r = loginUseCase.login(new LoginCommand(
                normalizar(req.correo()),
                req.contrasena()
        ));
        return ResponseEntity.ok(LoginResponseDto.from(r));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar un nuevo usuario")
    public ResponseEntity<LoginResponseDto> registro(@Valid @RequestBody RegistroRequest req) {
        AuthResult r = registrarUsuarioUseCase.registrar(new CreateUsuarioCommand(
                req.nombre().trim(),
                normalizar(req.correo()),
                req.contrasena(),
                req.rol(),
                req.telefono()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(LoginResponseDto.from(r));
    }

    @PostMapping("/google")
    @Operation(summary = "Iniciar sesión o registrarse con Google OAuth")
    public ResponseEntity<LoginResponseDto> google(@Valid @RequestBody GoogleLoginRequest req) {
        String hd = (properties.google().allowedDomain() == null || properties.google().allowedDomain().isBlank())
                ? req.hd()
                : properties.google().allowedDomain();
        AuthResult r = loginGoogleUseCase.loginGoogle(new GoogleLoginCommand(
                req.googleToken(), req.rol(), req.nonce(), hd
        ));
        return ResponseEntity.ok(LoginResponseDto.from(r));
    }

    @PostMapping("/github")
    @Operation(summary = "Iniciar sesión o registrarse con GitHub OAuth (Authorization Code flow)")
    public ResponseEntity<LoginResponseDto> github(@Valid @RequestBody GitHubLoginRequest req) {
        AuthResult r = loginGitHubUseCase.loginGitHub(new GitHubLoginCommand(
                req.code(), req.redirectUri(), req.rol()
        ));
        return ResponseEntity.ok(LoginResponseDto.from(r));
    }

    @GetMapping("/profile")
    @Operation(summary = "Obtener perfil del usuario autenticado")
    public ResponseEntity<UsuarioResponseDto> profile(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
        UsuarioView v = obtenerPerfilUseCase.perfil(user.id());
        return ResponseEntity.ok(UsuarioResponseDto.from(v));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token de acceso")
    public ResponseEntity<LoginResponseDto> refresh(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String refreshJti = null;
        String tokenFromBody = body == null ? null : (String) body.get("refreshToken");
        String raw = tokenFromBody != null ? tokenFromBody : refreshTokenHeader;
        if (raw != null) {
            try {
                Claims claims = tokenProvider.parsearAccessToken(raw);
                refreshJti = claims.getId();
            } catch (Exception ignored) {
            }
        }
        AuthResult r = refreshTokenUseCase.refresh(user.id(), refreshJti);
        return ResponseEntity.ok(LoginResponseDto.from(r));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión y revocar token")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (user == null || authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Token no proporcionado"));
        }
        String token = authHeader.substring(7);
        String jti = null;
        Instant exp = null;
        try {
            Claims claims = tokenProvider.parsearAccessToken(token);
            jti = claims.getId();
            if (claims.getExpiration() != null) {
                exp = claims.getExpiration().toInstant();
            }
        } catch (Exception ignored) {
        }
        logoutUseCase.logout(jti, exp);
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("message", "Logout exitoso. Sesión revocada.");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/verify-email/{token}")
    @Operation(summary = "Verificar email con token")
    public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable String token) {
        VerificarEmailUseCase.Resultado r = verificarEmailUseCase.verificar(token);
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("message", "Email verificado exitosamente");
        resp.put("userId", r.userId());
        resp.put("correo", r.correo());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/send-verification-email")
    @Operation(summary = "Reenviar email de verificación")
    public ResponseEntity<Map<String, String>> sendVerificationEmail(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        enviarVerificacionEmailUseCase.enviar(user.id(), user.correo());
        return ResponseEntity.ok(Map.of("message", "Email de verificación enviado"));
    }

    private static String normalizar(String correo) {
        return correo == null ? null : correo.trim().toLowerCase();
    }
}
