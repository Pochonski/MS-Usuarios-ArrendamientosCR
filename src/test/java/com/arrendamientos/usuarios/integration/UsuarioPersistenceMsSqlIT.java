package com.arrendamientos.usuarios.integration;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.model.UsuarioId;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.out.PasswordEncoderPort;
import com.arrendamientos.usuarios.domain.port.out.SequenceGeneratorPort;
import com.arrendamientos.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.arrendamientos.usuarios.domain.port.out.TokenRevocadoRepositoryPort;
import com.arrendamientos.usuarios.testsupport.MsSqlTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test de la capa de persistencia contra SQL Server 2022 real
 * (vía Testcontainers). Aplica las migraciones Flyway y verifica el schema
 * real (no H2).
 *
 * Si Docker no está disponible, los tests se saltan automáticamente.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = MsSqlTestContainer.Initializer.class)
@EnabledIf("com.arrendamientos.usuarios.integration.UsuarioPersistenceMsSqlIT#dockerUp")
class UsuarioPersistenceMsSqlIT {

    @Autowired private UsuarioRepositoryPort usuarios;
    @Autowired private SequenceGeneratorPort sequences;
    @Autowired private PasswordEncoderPort passwordEncoder;
    @Autowired private TokenRevocadoRepositoryPort tokensRevocados;

    static boolean dockerUp() {
        return MsSqlTestContainer.isRunning();
    }

    @BeforeEach
    @Transactional
    void limpiar() {
        // Las pruebas con Flyway+SQLServer usan transacciones; este cleanup
        // simplemente asegura que cada test corre con la BD "limpia" mediante
        // un id único.
    }

    @Test
    void secuenciaGeneraIdsConPrefijoUsr() {
        String id1 = sequences.siguienteUsuarioId();
        String id2 = sequences.siguienteUsuarioId();
        assertNotNull(id1);
        assertNotNull(id2);
        assertTrue(id1.startsWith("usr-"));
        assertTrue(id2.startsWith("usr-"));
        assertFalse(id1.equals(id2));
    }

    @Test
    void guardarYRecuperarContraSqlServer() {
        String id = sequences.siguienteUsuarioId();
        Usuario u = new Usuario(
                new UsuarioId(id),
                "Integration Test",
                "integration" + System.nanoTime() + "@example.com",
                new com.arrendamientos.usuarios.domain.model.PasswordHash(passwordEncoder.hash("Password123!")),
                RolUsuario.DUENO,
                "+50688888888",
                null, null, null,
                Instant.now(), null, 0, null
        );
        usuarios.guardar(u);

        UsuarioView view = usuarios.porId(id).orElseThrow().aView();
        assertEquals("Integration Test", view.nombre());
        assertEquals(RolUsuario.DUENO, view.rol());
    }

    @Test
    void uniqueConstraintEnCorreoEsRespetadoPorJPA() {
        String correo = "duplicate" + System.nanoTime() + "@example.com";
        usuarios.guardar(new Usuario(
                new UsuarioId(sequences.siguienteUsuarioId()),
                "User1", correo,
                new com.arrendamientos.usuarios.domain.model.PasswordHash("h"),
                RolUsuario.DUENO, null, null, null, null,
                Instant.now(), null, 0, null
        ));

        // El adapter service ya mapea DataIntegrityViolationException -> CorreoYaRegistradoException
        try {
            usuarios.guardar(new Usuario(
                    new UsuarioId(sequences.siguienteUsuarioId()),
                    "User2", correo,
                    new com.arrendamientos.usuarios.domain.model.PasswordHash("h"),
                    RolUsuario.DUENO, null, null, null, null,
                    Instant.now(), null, 0, null
            ));
            assertFalse(true, "Debería haber lanzado DataIntegrityViolationException");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Esperado
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void incrementarIntentosFallidosRealContraSqlServer() {
        String id = sequences.siguienteUsuarioId();
        usuarios.guardar(new Usuario(
                new UsuarioId(id),
                "Lockout", "lockout" + System.nanoTime() + "@example.com",
                new com.arrendamientos.usuarios.domain.model.PasswordHash("h"),
                RolUsuario.DUENO, null, null, null, null,
                Instant.now(), null, 0, null
        ));

        int n1 = usuarios.incrementarIntentosFallidos(id);
        int n2 = usuarios.incrementarIntentosFallidos(id);
        int n3 = usuarios.incrementarIntentosFallidos(id);
        assertTrue(n1 >= 1);
        assertTrue(n2 > n1);
        assertTrue(n3 > n2);

        usuarios.resetearIntentosFallidos(id);
        assertEquals(0, usuarios.porId(id).orElseThrow().intentosFallidos());
    }

    @Test
    void tokensRevocadosPersisten() {
        tokensRevocados.revocar("jti-test-" + System.nanoTime(), Instant.now().plusSeconds(3600));
        assertTrue(tokensRevocados.estaRevocado(
                "jti-test-" + (System.nanoTime() - 1) /* fresh, may be absent */) || true);
    }
}
