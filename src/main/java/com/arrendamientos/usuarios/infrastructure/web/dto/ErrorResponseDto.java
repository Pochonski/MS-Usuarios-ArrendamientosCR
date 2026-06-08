package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        String error,
        String message,
        Integer statusCode,
        List<String> details,
        Instant blockedUntil,
        Integer intentosFallidos,
        Integer intentosRestantes,
        String stack
) {
    public static ErrorResponseDto simple(String error, String message) {
        return new ErrorResponseDto(error, message, null, null, null, null, null, null);
    }

    public static ErrorResponseDto simple(String error, String message, int statusCode) {
        return new ErrorResponseDto(error, message, statusCode, null, null, null, null, null);
    }
}
