package com.arrendamientos.usuarios.infrastructure.web.advice;

import com.arrendamientos.usuarios.application.service.UsuarioService;
import com.arrendamientos.usuarios.domain.exception.CuentaBloqueadaException;
import com.arrendamientos.usuarios.domain.exception.DomainException;
import com.arrendamientos.usuarios.domain.exception.PermisoDenegadoException;
import com.arrendamientos.usuarios.infrastructure.web.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final boolean includeStack;

    public GlobalExceptionHandler(@Value("${spring.profiles.active:dev}") String profile) {
        this.includeStack = "dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponseDto> handleDomain(DomainException ex) {
        if (ex instanceof CuentaBloqueadaException c) {
            ErrorResponseDto body = new ErrorResponseDto(
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex.getStatus().value(),
                    null,
                    c.getBloqueadoHasta(),
                    c.getIntentosFallidos(),
                    c.getIntentosRestantes(),
                    null
            );
            return ResponseEntity.status(ex.getStatus()).body(body);
        }
        if (ex instanceof PermisoDenegadoException) {
            return ResponseEntity.status(ex.getStatus())
                    .body(ErrorResponseDto.simple("Forbidden", ex.getMessage(), ex.getStatus().value()));
        }
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponseDto.simple(ex.getClass().getSimpleName(), ex.getMessage(), ex.getStatus().value()));
    }

    @ExceptionHandler(UsuarioService.CredencialesInvalidasExceptionConIntentos.class)
    public ResponseEntity<ErrorResponseDto> handleCredencialesConIntentos(UsuarioService.CredencialesInvalidasExceptionConIntentos ex) {
        ErrorResponseDto body = new ErrorResponseDto(
                "CredencialesInvalidasException",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                null,
                null,
                ex.getIntentosFallidos(),
                ex.getIntentosRestantes(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ErrorResponseDto body = new ErrorResponseDto(
                "Validation Error",
                "Datos de entrada inválidos",
                HttpStatus.BAD_REQUEST.value(),
                details,
                null, null, null, null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegal(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.simple("BadRequest", ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalState(IllegalStateException ex) {
        // Config / setup errors (e.g. OAuth client credentials missing) — 503 Service Unavailable
        // is more accurate than 500 because it's a server-side config issue, not an unhandled crash.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponseDto.simple("ServiceUnavailable", ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE.value()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleBadJson(HttpMessageNotReadableException ex) {
        log.warn("JSON malformado: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.simple("BadRequest", "JSON malformado o cuerpo inválido", 400));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDto.simple("Forbidden", ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponseDto.simple("Unauthorized", ex.getMessage(), HttpStatus.UNAUTHORIZED.value()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.simple("Not Found", "Ruta " + ex.getHttpMethod() + " " + ex.getRequestURL() + " no encontrada", 404));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex) {
        log.error("Error no controlado", ex);
        ErrorResponseDto body = new ErrorResponseDto(
                "Error",
                includeStack ? ex.getMessage() : "Error interno del servidor",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null, null, null, null,
                includeStack ? stackToString(ex) : null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String stackToString(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append("\n");
        for (StackTraceElement e : ex.getStackTrace()) {
            sb.append("    at ").append(e).append("\n");
        }
        return sb.toString();
    }
}
