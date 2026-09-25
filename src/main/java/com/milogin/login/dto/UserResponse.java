package com.milogin.login.dto;

// DTO de salida con la información pública del usuario.
//NUNCA incluye el hash de la contraseña. Los roles se exponen como una lista de strings ("ROLE_ADMIN") porque el frontend solo necesita
//saber qué roles tiene para decidir qué panel mostrar.

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String nombre,
        Boolean activo,
        //Pondremos la lista de nombres de rol: ["ROLE_ADMIN y ROLE_USER"]. Usamos List (no Set) porque en JSON el orden es más natural
        //Y el volumen es más bajo
        List<String>roles
) {
}
