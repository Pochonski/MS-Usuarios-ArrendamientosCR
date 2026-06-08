package com.arrendamientos.usuarios.infrastructure.web;

import com.arrendamientos.usuarios.application.dto.UpdateUsuarioCommand;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.in.ActualizarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EliminarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerUsuarioUseCase;
import com.arrendamientos.usuarios.infrastructure.security.AuthenticatedUser;
import com.arrendamientos.usuarios.infrastructure.web.dto.PaginatedResponseDto;
import com.arrendamientos.usuarios.infrastructure.web.dto.UpdateUsuarioRequest;
import com.arrendamientos.usuarios.infrastructure.web.dto.UsuarioResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Usuarios", description = "CRUD de usuarios")
public class UsuarioController {

    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    private final ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    private final EliminarUsuarioUseCase eliminarUsuarioUseCase;

    public UsuarioController(
            ListarUsuariosUseCase listarUsuariosUseCase,
            ObtenerUsuarioUseCase obtenerUsuarioUseCase,
            ActualizarUsuarioUseCase actualizarUsuarioUseCase,
            EliminarUsuarioUseCase eliminarUsuarioUseCase) {
        this.listarUsuariosUseCase = listarUsuariosUseCase;
        this.obtenerUsuarioUseCase = obtenerUsuarioUseCase;
        this.actualizarUsuarioUseCase = actualizarUsuarioUseCase;
        this.eliminarUsuarioUseCase = eliminarUsuarioUseCase;
    }

    @GetMapping("/usuarios")
    @Operation(
            summary = "Listar usuarios",
            description = "Tres modos de uso (mutuamente excluyentes, en este orden de prioridad): " +
                    "1. Con query param 'email' → retorna array con usuarios cuyo correo empieza con ese prefijo. " +
                    "2. Con query param 'rol' → retorna array con todos los usuarios de ese rol. " +
                    "3. Sin filtros → retorna respuesta paginada (page=1, limit=20 por defecto; max limit=100)."
    )
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit) {
        if (email != null && !email.isBlank()) {
            List<UsuarioView> data = listarUsuariosUseCase.buscarPorPrefijoCorreo(email);
            return ResponseEntity.ok(data.stream().map(UsuarioResponseDto::from).toList());
        }
        if (rol != null && !rol.isBlank()) {
            List<UsuarioView> data = listarUsuariosUseCase.listarPorRol(rol);
            return ResponseEntity.ok(data.stream().map(UsuarioResponseDto::from).toList());
        }
        int p = page == null ? 1 : Math.max(1, page);
        int l = limit == null ? 20 : Math.min(100, Math.max(1, limit));
        var result = listarUsuariosUseCase.listarPaginado(p, l);
        return ResponseEntity.ok(PaginatedResponseDto.from(result));
    }

    @GetMapping("/usuario/{id}")
    @Operation(
            summary = "Obtener usuario por ID",
            description = "Retorna el perfil público de un usuario. 404 si no existe."
    )
    public ResponseEntity<UsuarioResponseDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(UsuarioResponseDto.from(obtenerUsuarioUseCase.porId(id)));
    }

    @PutMapping("/usuario/{id}")
    @Operation(
            summary = "Actualizar perfil del usuario autenticado",
            description = "Solo el dueño del perfil puede actualizarlo (id del path debe coincidir con el del JWT). " +
                    "Campos opcionales: solo se actualizan los que vienen en el body. " +
                    "Para cambiar el correo, el nuevo debe ser único en el sistema."
    )
    public ResponseEntity<UsuarioResponseDto> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateUsuarioRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        UpdateUsuarioCommand cmd = new UpdateUsuarioCommand(
                req.nombre(), req.correo(), req.telefono(), req.avatar()
        );
        UsuarioView v = actualizarUsuarioUseCase.actualizar(id, cmd, user.id());
        return ResponseEntity.ok(UsuarioResponseDto.from(v));
    }

    @DeleteMapping("/usuario/{id}")
    @Operation(
            summary = "Eliminar la cuenta del usuario autenticado",
            description = "Solo el dueño puede eliminar su propia cuenta (id del path debe coincidir con el del JWT). " +
                    "Esta acción es IRREVERSIBLE. Los tokens del usuario quedan inválidos automáticamente."
    )
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "No autenticado"));
        }
        eliminarUsuarioUseCase.eliminar(id, user.id());
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado correctamente"));
    }
}
