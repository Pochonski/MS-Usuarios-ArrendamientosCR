package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un nuevo usuario en la plataforma. " +
                     "El usuario creado recibe un email de verificación con un link que debe abrir antes de poder usar ciertas features.")
public record RegistroRequest(
        @NotBlank
        @Size(min = 2, max = 100)
        @Schema(description = "Nombre completo del usuario", example = "Carlos Ramírez", requiredMode = Schema.RequiredMode.REQUIRED)
        String nombre,

        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(description = "Correo electrónico único. Se normaliza a minúsculas.", example = "carlos.ramirez@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String correo,

        @NotBlank
        @Size(min = 8, max = 100)
        @Schema(description = "Contraseña (min 8 caracteres). Se hashea con BCrypt strength 10 antes de guardar.",
                example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String contrasena,

        @NotNull
        @Schema(description = "Rol del usuario en la plataforma", example = "DUENO", requiredMode = Schema.RequiredMode.REQUIRED)
        RolUsuario rol,

        @Pattern(regexp = "^\\+?[0-9]{8,12}$", message = "Teléfono inválido (8-12 dígitos, opcional +506)")
        @Schema(description = "Teléfono de contacto formato E.164 o nacional (8-12 dígitos, opcional +506)",
                example = "+50688888888")
        String telefono
) {
}
