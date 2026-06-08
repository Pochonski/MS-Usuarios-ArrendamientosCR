package com.arrendamientos.usuarios.web;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.in.ActualizarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EliminarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EnviarVerificacionEmailUseCase;
import com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginGoogleUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginUseCase;
import com.arrendamientos.usuarios.domain.port.in.LogoutUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerPerfilUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.RefreshTokenUseCase;
import com.arrendamientos.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.VerificarEmailUseCase;
import com.arrendamientos.usuarios.domain.port.out.GoogleTokenVerifierPort;
import com.arrendamientos.usuarios.testsupport.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ListarUsuariosUseCase listarUsuariosUseCase;
    @MockBean private ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    @MockBean private ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    @MockBean private EliminarUsuarioUseCase eliminarUsuarioUseCase;
    @MockBean private LoginUseCase loginUseCase;
    @MockBean private LoginGoogleUseCase loginGoogleUseCase;
    @MockBean private RegistrarUsuarioUseCase registrarUsuarioUseCase;
    @MockBean private ObtenerPerfilUseCase obtenerPerfilUseCase;
    @MockBean private LogoutUseCase logoutUseCase;
    @MockBean private VerificarEmailUseCase verificarEmailUseCase;
    @MockBean private EnviarVerificacionEmailUseCase enviarVerificacionEmailUseCase;
    @MockBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockBean private GoogleTokenVerifierPort googleVerifier;

    private static UsuarioView view(String id, RolUsuario rol) {
        return new UsuarioView(id, "Juan", id + "@example.com", rol, null, null,
                Instant.parse("2024-01-01T00:00:00Z"), null);
    }

    @Test
    void listarPaginadoDevuelve200ConJson() throws Exception {
        when(listarUsuariosUseCase.listarPaginado(1, 20))
                .thenReturn(new ListarUsuariosUseCase.ListadoPaginado<>(
                        List.of(view("usr-001", RolUsuario.DUENO), view("usr-002", RolUsuario.INQUILINO)),
                        1, 20, 2L, 1));

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.pagination.total").value(2))
                .andExpect(jsonPath("$.pagination.pages").value(1));
    }

    @Test
    void listarPorEmailDevuelveArray() throws Exception {
        when(listarUsuariosUseCase.buscarPorPrefijoCorreo("juan"))
                .thenReturn(List.of(view("usr-001", RolUsuario.DUENO)));

        mockMvc.perform(get("/api/usuarios?email=juan")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("usr-001"));
    }

    @Test
    void listarPorRolDevuelveArray() throws Exception {
        when(listarUsuariosUseCase.listarPorRol("dueno"))
                .thenReturn(List.of(view("usr-001", RolUsuario.DUENO)));

        mockMvc.perform(get("/api/usuarios?rol=dueno")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rol").value("dueno"));
    }

    @Test
    void getById200() throws Exception {
        when(obtenerUsuarioUseCase.porId("usr-001")).thenReturn(view("usr-001", RolUsuario.DUENO));

        mockMvc.perform(get("/api/usuario/usr-001")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usr-001"));
    }

    @Test
    void getById404SiNoExiste() throws Exception {
        when(obtenerUsuarioUseCase.porId("usr-XXX"))
                .thenThrow(new com.arrendamientos.usuarios.domain.exception.UsuarioNoEncontradoException("usr-XXX"));

        mockMvc.perform(get("/api/usuario/usr-XXX")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update200SiEsMismoUsuario() throws Exception {
        when(actualizarUsuarioUseCase.actualizar(anyString(), any(), anyString()))
                .thenReturn(view("usr-001", RolUsuario.DUENO));

        mockMvc.perform(put("/api/usuario/usr-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan P\"}")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isOk());
    }

    @Test
    void update403SiEsOtroUsuario() throws Exception {
        when(actualizarUsuarioUseCase.actualizar(anyString(), any(), anyString()))
                .thenThrow(new com.arrendamientos.usuarios.domain.exception.PermisoDenegadoException("x"));

        mockMvc.perform(put("/api/usuario/usr-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otro\"}")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void update400SiValidacionFalla() throws Exception {
        mockMvc.perform(put("/api/usuario/usr-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"A\"}")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete200() throws Exception {
        mockMvc.perform(delete("/api/usuario/usr-001")
                        .header("Authorization", "Bearer " + TestJwt.accessToken("usr-001", "x@x.com", RolUsuario.DUENO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuario eliminado correctamente"));

        verify(eliminarUsuarioUseCase).eliminar("usr-001", "usr-001");
    }

    @Test
    void deleteSinToken401() throws Exception {
        mockMvc.perform(delete("/api/usuario/usr-001"))
                .andExpect(status().isUnauthorized());
    }
}
