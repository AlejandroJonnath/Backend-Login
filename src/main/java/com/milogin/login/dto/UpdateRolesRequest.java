// Resumen: DTO del cuerpo para actualizar los roles de un usuario.
// Recibimos la lista de nombres de rol ("ROLE_ADMIN", etc.) tal como
// los maneja Spring Security. La validación de que existen y son
// asignables la hace el servicio.

package com.milogin.login.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateRolesRequest(

        // @NotEmpty rechaza null Y listas vacías. Un usuario sin roles
        // no tiene sentido en nuestro modelo: siempre debe tener al
        // menos uno (mínimo ROLE_USER).
        @NotEmpty(message = "Debe especificar al menos un rol")
        List<String> roles
) {
}