// Resumen: controlador REST que expone los endpoints de autenticación.
// /login valida credenciales y emite una cookie HttpOnly con el JWT.
// /logout borra la cookie. /me devuelve los datos del usuario autenticado.
// Este controlador NO tiene lógica de negocio: delega en AuthenticationManager
// y JwtService. Es la capa HTTP pura.

package com.milogin.login.controller;

import com.milogin.login.dto.LoginRequest;
import com.milogin.login.dto.UserResponse;
import com.milogin.login.security.JwtService;
import com.milogin.login.security.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final String cookieName;
    private final long expirationMs;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${jwt.cookie-name}") String cookieName,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
        this.expirationMs = expirationMs;
    }

    // POST /api/auth/login
    // Recibe username+password, valida con AuthenticationManager,
    // genera el JWT y lo mete en una cookie HttpOnly.
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        try {
            // authenticationManager.authenticate dispara el flujo estándar:
            //   1. CustomUserDetailsService.loadUserByUsername
            //   2. PasswordEncoder.matches(passwordPlano, hashGuardado)
            // Si algo falla, lanza AuthenticationException.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            // El principal es nuestro UserPrincipal (viene del UserDetailsService).
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // Generamos el JWT.
            String token = jwtService.generateToken(userPrincipal);

            // Construimos la cookie con ResponseCookie (API moderna de Spring).
            // ResponseCookie es fluent, inmutable, y maneja los flags de
            // seguridad correctamente.
            ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                    .httpOnly(true)                              // JavaScript NO puede leer la cookie (anti-XSS).
                    .secure(false)                               // ⚠️ true en producción (requiere HTTPS). false en local.
                    .path("/")                                   // Disponible en toda la app.
                    .maxAge(Duration.ofMillis(expirationMs))     // Debe coincidir con la exp del JWT.
                    .sameSite("Lax")                             // Lax: no viaja en peticiones cross-site (anti-CSRF).
                    .build();

            // Añadimos la cookie a la respuesta vía header Set-Cookie.
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Devolvemos los datos del usuario para que el frontend pueda
            // pintar su panel sin tener que llamar a /me inmediatamente.
            return ResponseEntity.ok(toUserResponse(userPrincipal));

        } catch (BadCredentialsException e) {
            // Credenciales incorrectas. Respondemos 401 sin dar pistas
            // sobre si el fallo fue el username o la contraseña
            // (mitiga enumeración de usuarios).
            return ResponseEntity.status(401).build();
        }
    }

    // POST /api/auth/logout
    // Borra la cookie seteándola con maxAge=0. El navegador la descarta.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Recreamos la misma cookie pero con maxAge=0 para que el
        // navegador la elimine inmediatamente. Debe coincidir en
        // nombre, path, y dominio con la original para sobrescribirla.
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)                                    // 0 = borrar.
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();           // 204 No Content.
    }

    // GET /api/auth/me
    // Devuelve el usuario autenticado leyendo el SecurityContext, que a su
    // vez fue poblado por el JwtAuthenticationFilter. El frontend usa este
    // endpoint al arrancar para saber si el usuario sigue logueado.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // @AuthenticationPrincipal inyecta el UserPrincipal del SecurityContext.
        // Si no está autenticado, Spring Security no llega aquí (401 antes).
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(toUserResponse(userPrincipal));
    }

    // Mapper privado: convierte UserPrincipal a UserResponse.
    // Mantener este mapeo dentro del controlador es aceptable mientras
    // la lógica sea trivial. Si crece, se extrae a un mapper dedicado
    // (patrón Mapper) en una fase posterior.
    private UserResponse toUserResponse(UserPrincipal userPrincipal) {
        var usuario = userPrincipal.getUsuario();
        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.getNombre())
                .toList();
        return new UserResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getActivo(),
                roles
        );
    }
}