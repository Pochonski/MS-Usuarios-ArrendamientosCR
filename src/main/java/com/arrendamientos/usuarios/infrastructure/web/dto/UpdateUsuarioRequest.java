package com.arrendamientos.usuarios.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUsuarioRequest(
        @Size(min = 2, max = 100) String nombre,
        @Email String correo,
        @Pattern(regexp = "^\\+?[0-9]{8,12}$", message = "Teléfono inválido (8-12 dígitos, opcional +506)")
        String telefono,
        String avatar
) {
}
