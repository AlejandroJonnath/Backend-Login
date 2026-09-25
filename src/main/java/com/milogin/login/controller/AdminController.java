// Resumen: endpoints del panel de administración. Requieren ROLE_ADMIN.
// Aquí exponemos la gestión de usuarios: listar y cambiar roles.
// Es el ejemplo más claro de RBAC dinámico: el admin puede modificar
// los roles de otros usuarios en tiempo real, y esos cambios impactan
// en los permisos sin reiniciar la aplicación.

package com.milogin.login.controller;

import com.milogin.login.dto.UpdateRolesRequest;
import com.milogin.login.dto.UserResponse;
import com.milogin.login.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Doble capa: URL + método.
public class AdminController {

    private final UsuarioService usuarioService;

    public AdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Panel principal del admin.
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Panel de administración",
                "seccion", "resumen del sistema"
        ));
    }

    // Lista todos los usuarios con sus roles. Solo el admin puede verlo.
    @GetMapping("/usuarios")
    public ResponseEntity<List<UserResponse>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    // Cambia los roles de un usuario concreto.
    // PUT porque reemplaza el conjunto completo de roles (idempotente).
    @PutMapping("/usuarios/{id}/roles")
    public ResponseEntity<UserResponse> actualizarRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolesRequest request
    ) {
        return ResponseEntity.ok(usuarioService.actualizarRoles(id, request.roles()));
    }
}