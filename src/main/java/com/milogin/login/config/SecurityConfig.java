// Resumen: configuración central de Spring Security.
// Define el PasswordEncoder (BCrypt), el AuthenticationManager que
// valida credenciales contra la BD, la cadena de filtros HTTP, CORS
// para el frontend en localhost:5173, y las reglas de autorización
// por ruta. Todo en un solo lugar para que sea fácil de auditar.

package com.milogin.login.config;

import com.milogin.login.security.CustomUserDetailsService;
import com.milogin.login.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
// @EnableMethodSecurity activa las anotaciones @PreAuthorize y @PostAuthorize
// en los controladores. Sin esto, las anotaciones se ignoran silenciosamente.
// En la Fase 4 lo usaremos intensamente.
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    // El PasswordEncoder es el encargado de hashear y verificar
    // contraseñas. Usamos BCrypt con la fuerza por defecto (10 rounds),
    // que es el estándar actual. NUNCA uses MD5/SHA-* para passwords:
    // son rápidos por diseño y por eso son malos para esto.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // DaoAuthenticationProvider conecta:
    //   - UserDetailsService: cómo cargar el usuario por username.
    //   - PasswordEncoder: cómo comparar contraseñas.
    // Spring Security lo usa internamente para el .authenticate() del login.
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // En Spring Security 6.3+ el UserDetailsService se pasa por
        // constructor (el setter está deprecado/eliminado en Security 7).
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // El AuthenticationManager es el punto de entrada para autenticar.
    // Lo usaremos en el AuthController: authenticationManager.authenticate(...)
    // para validar usuario+contraseña al hacer login.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // Configuración CORS: permite que el frontend (localhost:5173) llame
    // al backend (localhost:8080). allowCredentials(true) es OBLIGATORIO
    // para que las cookies viajen entre puertos; sin él, el navegador
    // descarta las cookies en peticiones cross-origin.
    // IMPORTANTE: allowCredentials(true) es INCOMPATIBLE con allowedOrigins("*").
    // Por eso listamos explícitamente los orígenes permitidos.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));      // Incluye Content-Type, etc.
        config.setAllowCredentials(true);            // Permite el envío de cookies.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Aplica a todas las rutas.
        return source;
    }

    // La cadena de filtros HTTP. Aquí se define el comportamiento
    // de seguridad de la aplicación.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS: habilita la configuración que definimos arriba.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF: la desactivamos porque nuestra API es stateless
                // y las cookies usan SameSite=Lax (mitigación CSRF moderna).
                // El token CSRF clásico (sincronizado en sesión) no aplica.
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless: Spring Security NO crea HttpSession. Cada
                // petición se autentica desde cero leyendo el JWT.
                // Esto es lo correcto para APIs que sirven a SPAs.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reglas de autorización por ruta.
                // El orden IMPORTA: la primera coincidencia gana.
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas: login y logout no requieren estar autenticado.
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        // /me requiere estar autenticado (lo dice explícitamente).
                        // Las reglas por rol las añadiremos en la Fase 4.
                        .requestMatchers("/api/auth/me").authenticated()
                        // Todo lo demás requiere autenticación por defecto.
                        // Esto es más seguro que permitir todo y luego proteger
                        // rutas específicas (whitelist vs blacklist).
                        .anyRequest().authenticated()
                )

                // Registramos nuestro proveedor de autenticación.
                .authenticationProvider(authenticationProvider())

                // Añadimos el filtro JWT ANTES del filtro estándar de
                // autenticación por usuario/password. Así, cuando la cadena
                // llegue al filtro estándar, el SecurityContext ya estará
                // poblado si el JWT era válido.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}