package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Login o registro vía Google OAuth. " +
                     "El backend verifica el id_token contra Google, crea la cuenta si no existe, y devuelve JWT propio de la plataforma.")
public record GoogleLoginRequest(
        @NotBlank
        @Size(max = 5000)
        @Schema(description = "id_token de Google (credential de Google Identity Services)",
                example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...", requiredMode = Schema.RequiredMode.REQUIRED)
        String googleToken,

        @Schema(description = "Rol deseado al crear cuenta nueva (si el usuario ya existe, se ignora)",
                example = "DUENO")
        RolUsuario rol,

        @Schema(description = "Nonce para validación CSRF (opcional, recomendado)")
        String nonce,

        @Schema(description = "Hosted domain (filtro opcional de Google Workspace). Si vacío, usa el default de config.",
                example = "example.com")
        String hd
) {
}
