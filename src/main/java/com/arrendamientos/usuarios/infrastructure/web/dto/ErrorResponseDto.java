package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estándar de error. Los campos opcionales (details, blockedUntil, intentosFallidos, intentosRestantes, stack) solo aparecen cuando son relevantes para el tipo de error.")
public record ErrorResponseDto(
        @Schema(description = "Tipo o categoría del error (ej: 'BadRequest', 'Unauthorized', 'CuentaBloqueadaException')",
                example = "CredencialesInvalidasException")
        String error,

        @Schema(description = "Mensaje legible para el usuario", example = "Credenciales inválidas")
        String message,

        @Schema(description = "HTTP status code", example = "401")
        Integer statusCode,

        @Schema(description = "Lista de errores de validación (solo presente en errores 400 de Bean Validation)")
        List<String> details,

        @Schema(description = "Fecha hasta la cual la cuenta está bloqueada (solo en CuentaBloqueadaException)",
                example = "2026-06-08T06:00:00Z")
        Instant blockedUntil,

        @Schema(description = "Cantidad de intentos fallidos acumulados (solo en CredencialesInvalidasException)")
        Integer intentosFallidos,

        @Schema(description = "Cantidad de intentos restantes antes del bloqueo (solo en CredencialesInvalidasException)")
        Integer intentosRestantes,

        @Schema(description = "Stack trace (solo en perfil dev/test, omitido en prod por seguridad)")
        String stack
) {
    public static ErrorResponseDto simple(String error, String message) {
        return new ErrorResponseDto(error, message, null, null, null, null, null, null);
    }

    public static ErrorResponseDto simple(String error, String message, int statusCode) {
        return new ErrorResponseDto(error, message, statusCode, null, null, null, null, null);
    }
}
