// Resumen: se ejecuta una sola vez al arrancar la aplicación.
// Si no existe ningún usuario "admin", lo crea con la contraseña
// hasheada correctamente y le asigna los 3 roles. Es idempotente:
// si el admin ya existe, no hace nada. Es la forma correcta de
// sembrar datos de arranque en desarrollo (nunca en migraciones SQL).

package com.milogin.login.config;

import com.milogin.login.domain.Rol;
import com.milogin.login.domain.Usuario;
import com.milogin.login.repository.RolRepository;
import com.milogin.login.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotencia: si el admin ya existe, no hacemos nada.
        if (usuarioRepository.existsByUsername("admin")) {
            log.info("DataInitializer: el usuario 'admin' ya existe, se omite la creación.");
            return;
        }

        // Buscamos los roles que Flyway ya sembró en V1__init.sql.
        // Si no existen, es un error de configuración: mejor fallar
        // ruidosamente que dejar la app en estado inconsistente.
        Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ROLE_ADMIN en la BD"));
        Rol rolEmprendedor = rolRepository.findByNombre("ROLE_EMPRENDEDOR")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ROLE_EMPRENDEDOR en la BD"));
        Rol rolUser = rolRepository.findByNombre("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ROLE_USER en la BD"));

        // Construimos el admin con Builder (Lombok) para dejar claro
        // qué campo es qué, sin depender del orden del constructor.
        Usuario admin = Usuario.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123")) // Hash BCrypt, NUNCA texto plano.
                .nombre("Administrador")
                .activo(true)
                // Le damos los 3 roles al admin para poder probar todos
                // los paneles con un solo usuario en desarrollo.
                .roles(Set.of(rolAdmin, rolEmprendedor, rolUser))
                .build();

        usuarioRepository.save(admin);

        log.info("=========================================================");
        log.info("DataInitializer: usuario 'admin' creado con éxito.");
        log.info("  Username: admin");
        log.info("  Password: admin123  (CÁMBIALA en producción)");
        log.info("=========================================================");
    }
}