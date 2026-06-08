package com.arrendamientos.usuarios.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciales para login con email y contraseña. " +
                     "El correo se normaliza a minúsculas antes de consultar la BD.")
public record LoginRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(description = "Correo electrónico del usuario", example = "carlos.ramirez@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String correo,

        @NotBlank
        @Size(min = 8, max = 100)
        @Schema(description = "Contraseña en texto plano (min 8 caracteres). Se hashea con BCrypt strength 10 antes de comparar.",
                example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String contrasena
) {
}
