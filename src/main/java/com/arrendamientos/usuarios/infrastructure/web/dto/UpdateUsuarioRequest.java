package com.arrendamientos.usuarios.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos opcionales para actualizar el perfil del usuario autenticado. " +
                     "Solo el dueño del perfil puede actualizarlo (validado en el controller por userId == authUserId).")
public record UpdateUsuarioRequest(
        @Size(min = 2, max = 100)
        @Schema(description = "Nuevo nombre del usuario", example = "Carlos Ramírez Solís")
        String nombre,

        @Email
        @Size(max = 255)
        @Schema(description = "Nuevo correo (debe ser único, no usado por otro usuario)",
                example = "carlos.ramirez.solís@email.com")
        String correo,

        @Pattern(regexp = "^\\+?[0-9]{8,12}$", message = "Teléfono inválido (8-12 dígitos, opcional +506)")
        @Schema(description = "Nuevo teléfono", example = "+50688888888")
        String telefono,

        @Size(max = 500)
        @Schema(description = "URL del avatar (puede ser https://... o una ruta en el CDN propio)",
                example = "https://lh3.googleusercontent.com/a/AGN...")
        String avatar
) {
}
