package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @NotBlank @Email String correo,
        @NotBlank @Size(min = 8) String contrasena,
        @NotNull RolUsuario rol,
        @Pattern(regexp = "^\\+?[0-9]{8,12}$", message = "Teléfono inválido (8-12 dígitos, opcional +506)")
        String telefono
) {
}
