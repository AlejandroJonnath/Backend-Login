// Resumen: servicio con la lógica de negocio relacionada a usuarios.
// Los controladores NO deben tocar repositorios directamente; siempre
// pasan por aquí. Esto mantiene la capa HTTP delgada y facilita los
// tests (podemos testear el servicio con mocks del repositorio sin
// arrancar un servidor web).

package com.milogin.login.service;

import com.milogin.login.domain.Rol;
import com.milogin.login.domain.Usuario;
import com.milogin.login.dto.UserResponse;
import com.milogin.login.exception.ResourceNotFoundException;
import com.milogin.login.repository.RolRepository;
import com.milogin.login.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    // Devuelve todos los usuarios con sus roles cargados, mapeados a DTOs.
    // @Transactional(readOnly = true) mantiene la sesión de Hibernate
    // abierta para que los roles (LAZY) puedan cargarse al mapear.
    @Transactional(readOnly = true)
    public List<UserResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    // Cambia los roles de un usuario. Recibe nombres de rol y devuelve
    // el usuario actualizado como DTO.
    @Transactional
    public UserResponse actualizarRoles(Long usuarioId, List<String> nombresRoles) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + usuarioId));

        // Buscamos cada rol en la BD. Si alguno no existe, fallamos con
        // un mensaje claro (400 implícito por ser un input inválido).
        Set<Rol> nuevosRoles = new HashSet<>();
        for (String nombre : nombresRoles) {
            Rol rol = rolRepository.findByNombre(nombre)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rol no encontrado: " + nombre));
            nuevosRoles.add(rol);
        }

        // Reemplazamos el set entero. Con Set + @ManyToMany, Hibernate
        // gestiona los INSERT/DELETE en la tabla intermedia de forma
        // eficiente comparando con el estado previo.
        usuario.setRoles(nuevosRoles);
        usuarioRepository.save(usuario);

        return toUserResponse(usuario);
    }

    // Mapper privado: convierte entidad -> DTO. Es el mismo patrón que
    // usamos en AuthController, pero aquí es el dueño natural del mapeo.
    // En la Fase 7 podríamos extraerlo a una clase Mapper dedicada.
    private UserResponse toUserResponse(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getNombre)
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