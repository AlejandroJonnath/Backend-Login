// Resumen: manejador global de excepciones con @RestControllerAdvice.
// Intercepta las excepciones que burbujean desde los controladores
// y devuelve respuestas JSON uniformes. Evita que cada controlador
// tenga que hacer try/catch manual, y evita que el cliente reciba
// el HTML de error por defecto de Spring.

package com.milogin.login.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Captura errores de @Valid en los @RequestBody. Agrupa los mensajes
    // por campo para que el frontend pueda mostrarlos junto a cada input.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> erroresPorCampo = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            erroresPorCampo.put(fe.getField(), fe.getDefaultMessage());
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Error de validación", erroresPorCampo);
    }

    // Captura cuando un recurso no existe (nuestra excepción).
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    // Captura AccessDeniedException (403): el usuario está autenticado
    // pero no tiene el rol requerido. Lo más útil es devolver un mensaje
    // claro para que el frontend muestre "Acceso denegado".
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN,
                "No tienes permisos para acceder a este recurso", null);
    }

    // Captura AuthenticationException (401): fallo al autenticar
    // (credenciales malas). La lanzamos desde AuthController, pero aquí
    // unificamos el formato.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthFailure(AuthenticationException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED,
                "Credenciales incorrectas", null);
    }

    // Catch-all: cualquier excepción no manejada específicamente.
    // Logueamos el stack trace completo (útil para debug) pero devolvemos
    // un mensaje genérico al cliente (no exponer detalles internos).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Excepción no manejada", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor", null);
    }

    // Helper privado para construir el JSON de error con formato uniforme:
    // { timestamp, status, error, detalles(opcional) }
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, Object detalles
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        if (detalles != null) {
            body.put("detalles", detalles);
        }
        return ResponseEntity.status(status).body(body);
    }
}
/*
* los errores de autorización (403) y autenticación (401) que provienen del filtro JWT
* o del SecurityFilterChain (antes de llegar al controlador) no pasan por @RestControllerAdvice.
* Los manejaremos por separado en la Fase 8 con un AuthenticationEntryPoint y un AccessDeniedHandler personalizados.
*  Para los errores que sí vienen de los controladores (por ejemplo, @PreAuthorize a nivel de método), esta clase sí actúa.
* */