package com.milogin.login.repository;
// Repositorio Spring Data JPA para Rol.
// Además del CRUD que ya hereda de JpaRepository, necesitamos poder
// buscar por nombre, por ejemplo al asignar roles desde un endpoint
// de administración en fases posteriores.

import com.milogin.login.domain.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    // SELECT * FROM roles WHERE nombre = ?
    Optional<Rol> findByNombre(String nombre);

}
