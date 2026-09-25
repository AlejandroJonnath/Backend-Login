// Resumen: excepción de dominio lanzada cuando un recurso solicitado
// no existe. Spring la captura en GlobalExceptionHandler y la traduce
// a una respuesta HTTP 404 con un JSON consistente.

package com.milogin.login.exception;

public class ResourceNotFoundException extends RuntimeException {

    // Heredamos de RuntimeException (unchecked) para no contaminar
    // las firmas de los métodos con "throws". El manejo centralizado
    // ocurre en GlobalExceptionHandler.
    public ResourceNotFoundException(String message) {
        super(message);
    }
}