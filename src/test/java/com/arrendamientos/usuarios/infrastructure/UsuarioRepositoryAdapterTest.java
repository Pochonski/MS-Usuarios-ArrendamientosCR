package com.arrendamientos.usuarios.infrastructure.persistence;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.model.UsuarioId;
import com.arrendamientos.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.arrendamientos.usuarios.infrastructure.persistence.adapter.UsuarioRepositoryAdapter;
import com.arrendamientos.usuarios.infrastructure.persistence.entity.UsuarioEntity;
import com.arrendamientos.usuarios.infrastructure.persistence.mapper.UsuarioMapper;
import com.arrendamientos.usuarios.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@Import({UsuarioRepositoryAdapter.class, UsuarioMapper.class})
class UsuarioRepositoryAdapterTest {

    @Autowired private TestEntityManager em;
    @Autowired private UsuarioJpaRepository jpa;
    @Autowired private UsuarioRepositoryPort port;

    @BeforeEach
    void setUp() {
        // Cada test arranca con la tabla vacía
    }

    private UsuarioEntity insertar(String id, String correo, String hash, RolUsuario rol) {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(id);
        e.setNombre("Test");
        e.setCorreo(correo);
        e.setContrasenaHash(hash);
        e.setRol(rol.getValor());
        e.setFechaRegistro(Instant.now());
        return em.persistAndFlush(e);
    }

    @Test
    void guardarYRecuperar() {
        Usuario u = new Usuario(
                new UsuarioId("usr-001"),
                "Juan",
                "juan@example.com",
                new com.arrendamientos.usuarios.domain.model.PasswordHash("$2a$10$h"),
                RolUsuario.DUENO,
                null, null, null, null,
                Instant.now(), null, 0, null
        );
        port.guardar(u);

        Optional<Usuario> r = port.porId("usr-001");
        assertTrue(r.isPresent());
        assertEquals("Juan", r.get().nombre());
        assertEquals("juan@example.com", r.get().correo());
    }

    @Test
    void porCorreoEncuentraUsuario() {
        insertar("usr-001", "juan@example.com", "$2a$10$h", RolUsuario.DUENO);
        Optional<Usuario> r = port.porCorreo("juan@example.com");
        assertTrue(r.isPresent());
        assertEquals("usr-001", r.get().id().value());
    }

    @Test
    void porCorreoNoEncuentra() {
        insertar("usr-001", "juan@example.com", "$2a$10$h", RolUsuario.DUENO);
        assertFalse(port.porCorreo("nadie@example.com").isPresent());
    }

    @Test
    void porGoogleIdEncuentraUsuario() {
        UsuarioEntity e = insertar("usr-001", "juan@example.com", null, RolUsuario.DUENO);
        e.setGoogleId("google-123");
        em.persistAndFlush(e);

        Optional<Usuario> r = port.porGoogleId("google-123");
        assertTrue(r.isPresent());
    }

    @Test
    void porPrefijoCorreoEncuentraResultados() {
        insertar("usr-001", "juan@example.com", null, RolUsuario.DUENO);
        insertar("usr-002", "juana@example.com", null, RolUsuario.INQUILINO);
        insertar("usr-003", "pedro@example.com", null, RolUsuario.DUENO);

        assertEquals(2, port.porPrefijoCorreo("juan").size());
    }

    @Test
    void porRolFiltra() {
        insertar("usr-001", "a@e.com", null, RolUsuario.DUENO);
        insertar("usr-002", "b@e.com", null, RolUsuario.INQUILINO);
        insertar("usr-003", "c@e.com", null, RolUsuario.DUENO);

        assertEquals(2, port.porRol(RolUsuario.DUENO).size());
        assertEquals(1, port.porRol(RolUsuario.INQUILINO).size());
    }

    @Test
    void paginadoDevuelveOffsetCorrecto() {
        for (int i = 0; i < 5; i++) {
            insertar(String.format("usr-%03d", i + 1), "u" + i + "@e.com", null, RolUsuario.DUENO);
        }
        var p1 = port.paginado(1, 2);
        var p2 = port.paginado(2, 2);
        var p3 = port.paginado(3, 2);
        assertEquals(2, p1.size());
        assertEquals(2, p2.size());
        assertEquals(1, p3.size());
    }

    @Test
    void contarDevuelveTotal() {
        for (int i = 0; i < 3; i++) {
            insertar(String.format("usr-%03d", i + 1), "u" + i + "@e.com", null, RolUsuario.DUENO);
        }
        assertEquals(3L, port.contar());
    }

    @Test
    void existeCorreoExcluyendoId() {
        insertar("usr-001", "a@e.com", null, RolUsuario.DUENO);
        insertar("usr-002", "b@e.com", null, RolUsuario.DUENO);

        assertTrue(port.existeCorreoExcluyendoId("a@e.com", "usr-002"));
        assertFalse(port.existeCorreoExcluyendoId("a@e.com", "usr-001"));
    }

    @Test
    void eliminarRetornaTrueSiExiste() {
        insertar("usr-001", "a@e.com", null, RolUsuario.DUENO);
        assertTrue(port.eliminar("usr-001"));
        assertFalse(port.eliminar("usr-001"));
    }

    @Test
    void actualizarUltimoLogin() {
        insertar("usr-001", "a@e.com", null, RolUsuario.DUENO);
        // H2 DATETIME2 tiene precisión de milisegundos (no nanos)
        Instant nuevo = Instant.now().plusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        port.actualizarUltimoLogin("usr-001", nuevo);
        em.clear();
        assertEquals(nuevo, port.porId("usr-001").get().ultimoLogin());
    }

    @Test
    void incrementarIntentosFallidosIncrementa() {
        insertar("usr-001", "a@e.com", null, RolUsuario.DUENO);
        int n1 = port.incrementarIntentosFallidos("usr-001");
        int n2 = port.incrementarIntentosFallidos("usr-001");
        int n3 = port.incrementarIntentosFallidos("usr-001");
        assertEquals(1, n1);
        assertEquals(2, n2);
        assertEquals(3, n3);
    }

    @Test
    void resetearIntentosFallidosLimpia() {
        insertar("usr-001", "a@e.com", null, RolUsuario.DUENO);
        port.incrementarIntentosFallidos("usr-001");
        port.incrementarIntentosFallidos("usr-001");
        port.resetearIntentosFallidos("usr-001");

        em.clear();
        assertEquals(0, port.porId("usr-001").get().intentosFallidos());
    }
}
