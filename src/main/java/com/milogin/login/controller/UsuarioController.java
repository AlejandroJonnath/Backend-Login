// Resumen: endpoints del panel de usuario normal. Solo requieren
// estar autenticado; cualquier rol vale. Devuelven datos personales
// del usuario autenticado.

package com.milogin.login.controller;

import com.milogin.login.dto.UserResponse;
import com.milogin.login.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    // Endpoint de bienvenida del panel de usuario.
    // No hace nada especial: solo demuestra que el usuario está
    // autenticado y devuelve su propio perfil.
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        // @AuthenticationPrincipal nos inyecta el UserPrincipal del contexto.
        var usuario = userPrincipal.getUsuario();
        return ResponseEntity.ok(Map.of(
                "mensaje", "Bienvenido a tu panel de usuario, " + usuario.getNombre(),
                "usuario", usuario.getUsername(),
                "roles", usuario.getRoles().stream().map(r -> r.getNombre()).toList()
        ));
    }

    // Endpoint que devuelve solo el perfil del usuario autenticado.
    // Es análogo a /api/auth/me pero en su propio namespace, para que
    // cada panel tenga su propia API coherente.
    @GetMapping("/perfil")
    public ResponseEntity<UserResponse> perfil(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        var u = userPrincipal.getUsuario();
        return ResponseEntity.ok(new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getNombre(), u.getActivo(),
                u.getRoles().stream().map(r -> r.getNombre()).toList()
        ));
    }
}