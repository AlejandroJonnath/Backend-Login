// Resumen: endpoints del panel de emprendedor. Requieren el rol
// ROLE_EMPRENDEDOR o ROLE_ADMIN (ya lo fuerza SecurityConfig vía la
// URL). Añadimos @PreAuthorize como segunda capa, defensa en profundidad.

package com.milogin.login.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/emprendedor")
@PreAuthorize("hasAnyRole('EMPRENDEDOR', 'ADMIN')") // Aplica a todos los métodos de esta clase.
public class EmprendedorController {

    // Panel principal del emprendedor. En una app real aquí vendrían
    // sus productos, ventas, métricas, etc. Por ahora, solo un mensaje.
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Panel de emprendedor",
                "usuario", userDetails.getUsername(),
                "seccion", "resumen de negocio"
        ));
    }

    // Ejemplo de acción exclusiva del emprendedor: publicar un producto.
    // POST porque crea algo. En la Fase 6 lo usaremos desde el frontend.
    @PostMapping("/productos")
    public ResponseEntity<Map<String, Object>> publicarProducto() {
        return ResponseEntity.ok(Map.of(
                "mensaje", "Producto publicado (simulado)",
                "estado", "ok"
        ));
    }
}