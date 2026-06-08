package com.arrendamientos.usuarios.web;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.in.LoginUseCase;
import com.arrendamientos.usuarios.domain.port.in.LogoutUseCase;
import com.arrendamientos.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.out.GoogleTokenVerifierPort;
import com.arrendamientos.usuarios.infrastructure.config.AuthMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricsEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MeterRegistry meterRegistry;
    @Autowired private AuthMetrics authMetrics;
    @MockBean private LoginUseCase loginUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.LoginGoogleUseCase loginGoogleUseCase;
    @MockBean private RegistrarUsuarioUseCase registrarUsuarioUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.ObtenerPerfilUseCase obtenerPerfilUseCase;
    @MockBean private LogoutUseCase logoutUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.VerificarEmailUseCase verificarEmailUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.EnviarVerificacionEmailUseCase enviarVerificacionEmailUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.RefreshTokenUseCase refreshTokenUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase listarUsuariosUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    @MockBean private com.arrendamientos.usuarios.domain.port.in.EliminarUsuarioUseCase eliminarUsuarioUseCase;
    @MockBean private GoogleTokenVerifierPort googleVerifier;

    @Test
    void healthEndpointExponeEstado() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void authMetricsBeanRegistrado() {
        assertNotNull(authMetrics);
        assertNotNull(meterRegistry);
    }

    @Test
    void countersSeRegistranEnMeterRegistryCompartido() {
        double before = meterRegistry.find("auth.login.success").counter().count();
        authMetrics.loginSuccess();
        double after = meterRegistry.find("auth.login.success").counter().count();
        assertEquals(1.0, after - before, 0.0001);
    }
}
