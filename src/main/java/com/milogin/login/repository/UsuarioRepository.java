package com.milogin.login.repository;
// Repositorio Spring Data JPA para Usuario.
// JpaRepository ya nos da CRUD completo (save, findById, findAll,
// deleteById, count, etc.). Aquí solo declaramos queries derivadas
// del nombre del método, que Spring Data resuelve automáticamente.

import com.milogin.login.domain.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring Data genera:
    //   SELECT * FROM usuarios WHERE username = ?
    Optional<Usuario> findByUsername(String username);

    // SELECT * FROM usuarios WHERE email = ?
    Optional<Usuario> findByEmail(String email);

    // SELECT COUNT(*) > 0 FROM usuarios WHERE username = ?
    boolean existsByUsername(String username);

    // SELECT COUNT(*) > 0 FROM usuarios WHERE email = ?
    boolean existsByEmail(String email);

    // @EntityGraph fuerza a cargar los roles junto con el usuario en la
    // misma consulta (LEFT JOIN), evitando la LazyInitializationException
    // al intentar leer los roles fuera de la transacción. Lo usaremos
    // en el login y en /api/auth/me. El nombre "roles" debe coincidir
    // con el atributo de la entidad Usuario.
    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findWithRolesByUsername(String username);
}
