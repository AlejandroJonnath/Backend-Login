// Resumen: implementación de UserDetailsService que carga usuarios
// desde PostgreSQL. Spring Security la usa durante la autenticación
// para buscar al usuario por su username y comparar la contraseña
// hasheada. También la usaremos en el filtro JWT para reconstruir
// el SecurityContext en cada petición.

package com.milogin.login.security;

import com.milogin.login.domain.Usuario;
import com.milogin.login.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    // Inyección por constructor: preferida sobre @Autowired en campos
    // porque hace las dependencias explícitas y facilita los tests.
    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Único método del contrato. Spring Security lo invoca con el
    // username introducido en el login.
    //
    // @Transactional(readOnly = true) mantiene la sesión de Hibernate
    // abierta mientras corre el método. Es NECESARIO porque los roles
    // son LAZY y los leemos justo después dentro del UserPrincipal.
    // Sin esto, con open-in-view=false tendríamos LazyInitializationException.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Usamos la variante con @EntityGraph para traer los roles
        // en la misma consulta (JOIN FETCH), en lugar de N+1.
        Usuario usuario = usuarioRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));
        return new UserPrincipal(usuario);
    }
}