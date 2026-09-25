// Resumen: adaptador que envuelve nuestra entidad Usuario y la expone
// como UserDetails (el contrato de Spring Security). Esta clase es la
// que Spring Security maneja dentro del SecurityContext durante toda
// la petición HTTP.

package com.milogin.login.security;

import com.milogin.login.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    // Guardamos la entidad completa para poder exponer datos extra
    // (email, nombre, id) desde el controlador sin recargar de BD.
    private final transient Usuario usuario;

    // Constructor simple: recibe la entidad ya cargada desde el servicio.
    public UserPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    // Getter para que el AuthController pueda construir el UserResponse
    // con datos del usuario autenticado sin tener que ir a la BD de nuevo.
    public Usuario getUsuario() {
        return usuario;
    }

    // Autoridades (roles/permisos). Spring Security espera GrantedAuthority.
    // Convertimos cada Rol.nombre ("ROLE_ADMIN") a SimpleGrantedAuthority.
    // El prefijo "ROLE_" es obligatorio para que hasRole("ADMIN") funcione;
    // si usaramos hasAuthority("ADMIN") podríamos omitirlo, pero hasRole
    // es más legible y es lo que usaremos en la Fase 4.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority(rol.getNombre()))
                .toList();
    }

    // Contraseña (hash BCrypt) que Spring Security comparará.
    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    // Identificador de login. En nuestro caso usamos el username.
    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    // Los 4 métodos siguientes controlan si la cuenta está "habilitada".
    // Los mapeamos al campo `activo` de la entidad. Si en el futuro
    // añadimos bloqueo por intentos fallidos, aquí retornaríamos
    // !usuario.isBloqueado(), etc.

    @Override
    public boolean isAccountNonExpired() {
        return true; // No manejamos expiración de cuentas todavía.
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // No manejamos bloqueo por intentos todavía.
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // No manejamos expiración de credenciales.
    }

    @Override
    public boolean isEnabled() {
        // Si el admin desactiva al usuario, no puede iniciar sesión.
        return Boolean.TRUE.equals(usuario.getActivo());
    }
}