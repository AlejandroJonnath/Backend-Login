// Resumen: filtro Servlet que se ejecuta una vez por petición HTTP.
// Su único trabajo es leer el JWT desde la cookie, validarlo, y si es
// correcto, rellenar el SecurityContext con la autenticación.
// Si no hay cookie o el token es inválido, simplemente deja pasar la
// petición sin autenticar: será el SecurityFilterChain quien decida
// si esa ruta requería autenticación (y responderá 401/403 si aplica).

package com.milogin.login.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // OncePerRequestFilter garantiza que este filtro se ejecute UNA sola
    // vez por petición, incluso si hay reenvíos internos (forwards).
    // Es la clase base correcta para cualquier filtro en Spring.

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final String cookieName;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            @Value("${jwt.cookie-name}") String cookieName
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extraemos el token de la cookie. Si no hay, seguimos sin
        //    autenticar. Esto es normal para rutas públicas (/login).
        String token = extractTokenFromCookies(request);

        // 2. Si no hay token, o ya hay una autenticación en el contexto
        //    (por ejemplo, por otro filtro anterior), no hacemos nada.
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Extraemos el username del token SIN consultar la BD.
            String username = jwtService.extractUsername(token);

            if (username != null) {
                // 4. Solo aquí cargamos el usuario desde la BD. Lo hacemos
                //    porque necesitamos un UserDetails para validar la
                //    firma+expiración y para que quede en el SecurityContext.
                //    En una app de altísimo tráfico podríamos evitar esta
                //    consulta reconstruyendo el UserDetails desde los claims
                //    del token; aquí priorizamos claridad y robustez.
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    // 5. Creamos el objeto Authentication y lo metemos en
                    //    el SecurityContext. A partir de aquí, Spring Security
                    //    considera la petición "autenticada" para el resto
                    //    de la cadena. Le pasamos las authorities (roles) para
                    //    que las anotaciones @PreAuthorize funcionen.
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                        // Credenciales: null tras autenticar.
                                    userDetails.getAuthorities() // Roles del usuario.
                            );
                    // Detalles extra (IP, sessionId); útil para auditoría.
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // Si cualquier cosa falla (token corrupto, usuario borrado,
            // firma inválida), limpiamos el contexto y dejamos que la
            // cadena decida. NO respondemos aquí con 401: eso lo hace
            // el SecurityFilterChain según las reglas de la ruta.
            SecurityContextHolder.clearContext();
        }

        // 6. Continuar la cadena. Pase lo que pase, siempre se llama.
        filterChain.doFilter(request, response);
    }

    // Busca en las cookies de la petición la que se llama `cookieName`.
    // Devuelve null si no existe. Encapsular esta lógica aquí mantiene
    // doFilterInternal más legible.
    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}