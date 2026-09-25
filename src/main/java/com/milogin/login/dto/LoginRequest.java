package com.milogin.login.dto;

//DTO que representa el cuerpo JSON del endpoint de login.
//Un "record" de Java es inmutable por defecto así que es perfecto para los DTOs:
//sin setters, sin estado, sin efectos colaterales.
//Las anotaciones de Bean Validation (@NotBlank) hacen que Spring rechace peticiones mal formadas antes de tocar la capa de servicio.


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        //el notblank rechaza el nill, cadena vacía o solo espacios
        @NotBlank(message = "El nombre de usuario es obligatorio")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        //Solo vamos a validar la longuitud mínima; el hashing se hace en el servicio
        @Size(min = 4, message = "La contraseña debe tener mínimo 4 caracteres")
        String password
) {
}
