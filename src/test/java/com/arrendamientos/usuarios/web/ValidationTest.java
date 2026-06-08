package com.arrendamientos.usuarios.web;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.infrastructure.web.dto.LoginRequest;
import com.arrendamientos.usuarios.infrastructure.web.dto.RegistroRequest;
import com.arrendamientos.usuarios.infrastructure.web.dto.UpdateUsuarioRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    private long countViolations(Object dto, String field) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        return violations.stream().filter(v -> v.getPropertyPath().toString().equals(field)).count();
    }

    // -------- LOGIN --------

    @Test
    void loginRequestValido() {
        LoginRequest r = new LoginRequest("user@example.com", "Password123!");
        assertEquals(0, validator.validate(r).size());
    }

    @Test
    void loginRequestCorreoInvalido() {
        LoginRequest r = new LoginRequest("no-es-correo", "Password123!");
        assertTrue(countViolations(r, "correo") > 0);
    }

    @Test
    void loginRequestContrasenaVacia() {
        LoginRequest r = new LoginRequest("user@example.com", "");
        assertTrue(countViolations(r, "contrasena") > 0);
    }

    @Test
    void loginRequestCorreoVacio() {
        LoginRequest r = new LoginRequest("", "Password123!");
        assertTrue(countViolations(r, "correo") > 0);
    }

    // -------- REGISTRO --------

    @Test
    void registroRequestValido() {
        RegistroRequest r = new RegistroRequest("Juan Pérez", "j@e.com", "Password123!", RolUsuario.DUENO, "+50688888888");
        assertEquals(0, validator.validate(r).size());
    }

    @Test
    void registroRequestNombreCorto() {
        RegistroRequest r = new RegistroRequest("A", "j@e.com", "Password123!", RolUsuario.DUENO, null);
        assertTrue(countViolations(r, "nombre") > 0);
    }

    @Test
    void registroRequestNombreLargo() {
        RegistroRequest r = new RegistroRequest("A".repeat(101), "j@e.com", "Password123!", RolUsuario.DUENO, null);
        assertTrue(countViolations(r, "nombre") > 0);
    }

    @Test
    void registroRequestContrasenaCorta() {
        RegistroRequest r = new RegistroRequest("Juan", "j@e.com", "1234567", RolUsuario.DUENO, null);
        assertTrue(countViolations(r, "contrasena") > 0);
    }

    @Test
    void registroRequestRolNull() {
        RegistroRequest r = new RegistroRequest("Juan", "j@e.com", "Password123!", null, null);
        assertTrue(countViolations(r, "rol") > 0);
    }

    @Test
    void registroRequestTelefonoInvalido() {
        RegistroRequest r = new RegistroRequest("Juan", "j@e.com", "Password123!", RolUsuario.DUENO, "abc");
        assertTrue(countViolations(r, "telefono") > 0);
    }

    @Test
    void registroRequestTelefonoValido() {
        RegistroRequest r1 = new RegistroRequest("Juan", "j@e.com", "Password123!", RolUsuario.DUENO, "+50688888888");
        RegistroRequest r2 = new RegistroRequest("Juan", "j@e.com", "Password123!", RolUsuario.DUENO, "88888888");
        assertEquals(0, validator.validate(r1).size());
        assertEquals(0, validator.validate(r2).size());
    }

    @Test
    void registroRequestTelefonoOpcional() {
        RegistroRequest r = new RegistroRequest("Juan", "j@e.com", "Password123!", RolUsuario.DUENO, null);
        assertEquals(0, validator.validate(r).size());
    }

    // -------- UPDATE --------

    @Test
    void updateRequestTodosLosCamposOpcionales() {
        UpdateUsuarioRequest r = new UpdateUsuarioRequest(null, null, null, null);
        assertEquals(0, validator.validate(r).size());
    }

    @Test
    void updateRequestCorreoInvalido() {
        UpdateUsuarioRequest r = new UpdateUsuarioRequest(null, "no-correo", null, null);
        assertTrue(countViolations(r, "correo") > 0);
    }

    @Test
    void updateRequestNombreCorto() {
        UpdateUsuarioRequest r = new UpdateUsuarioRequest("A", null, null, null);
        assertTrue(countViolations(r, "nombre") > 0);
    }

    @Test
    void updateRequestTelefonoInvalido() {
        UpdateUsuarioRequest r = new UpdateUsuarioRequest(null, null, "telefono-malo", null);
        assertTrue(countViolations(r, "telefono") > 0);
    }
}
