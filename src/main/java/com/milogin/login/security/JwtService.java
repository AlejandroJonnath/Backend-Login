// Resumen: servicio que encapsula la creación y validación de JWTs.
// Firma con HMAC-SHA256 (HS256) usando la clave secreta del properties.
// No conoce HTTP ni cookies: solo Strings de entrada y salida.
// Este aislamiento facilita los tests unitarios: le pasas un username
// y verificas que el token generado lo contiene.

package com.milogin.login.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    // Clave secreta (HS256). La inyectamos desde el properties.
    private final SecretKey signingKey;

    // Tiempo de vida del token en milisegundos.
    private final long expirationMs;

    // Constructor: Spring inyecta los valores del properties vía @Value.
    // Convertimos el secreto (String) a SecretKey con Keys.hmacShaKeyFor,
    // que valida que tenga al menos 256 bits (32 bytes). Si es más corta,
    // lanza una excepción al arrancar: mejor fallar rápido que en runtime.
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Genera un token firmado para el usuario autenticado.
    // Además del subject (username), añadimos un claim custom "roles"
    // con la lista de roles. Esto nos permite reconstruir el
    // SecurityContext sin golpear la BD en cada petición (optimización
    // clave: solo consultamos la BD en el login).
    public String generateToken(UserDetails userDetails) {
        // Extraemos los nombres de rol ("ROLE_ADMIN", etc.) del UserDetails.
        List<String> roles = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())            // "sub" claim: a quién pertenece el token.
                .claim("roles", roles)                         // Claim custom con los roles.
                .issuedAt(now)                                 // "iat": cuándo se emitió.
                .expiration(expiry)                            // "exp": cuándo expira.
                .signWith(signingKey)                          // Firma con HS256.
                .compact();                                    // Serializa a String (header.payload.signature).
    }

    // Extrae el username (subject) del token.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Verifica que el token corresponda al usuario y no esté expirado.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            // Cualquier excepción de jjwt (firma inválida, formato corrupto,
            // expirado) significa token no válido. Devolvemos false en
            // lugar de propagar la excepción: el filtro solo necesita saber
            // sí o no.
            return false;
        }
    }

    // Comprueba la fecha de expiración.
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // Helper genérico para extraer cualquier claim. El Function permite
    // tipar el retorno sin duplicar código para cada claim.
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)     // Verifica la firma con la misma clave.
                .build()
                .parseSignedClaims(token)   // Parsea el JWT y valida firma+exp.
                .getPayload();
        return resolver.apply(claims);
    }
}