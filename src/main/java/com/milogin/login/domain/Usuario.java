package com.milogin.login.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity //Entidad JPA (tabla usuarios)
@Table(name = "usuarios") //nombre de la tabla
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //Autoincremento delegado a Postgres
    private long id;

    @Column(nullable = false, unique = true, length = 50) //ID
    private String username;

    @Column(nullable = false, unique = true, length = 100) //Correo
    private String email;

    @Column(nullable = false, length = 255) //Contraseña
    private String password;

    @Column(nullable = false, length = 100) //Nombre de usuario
    private String nombre;

    @Column(nullable = false) //sI EL USUARIO ESTÁ ACTIVO
    @Builder.Default //Por defecto estará activo
    private Boolean activo = true; //Aquí usaremos "borrado pasivo" donde no eliminamos directamente, solo lo ocultamos

    @CreationTimestamp //Hibernate lo settea automáticamente al INSERT
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedTime;

    //Ahora debemos tener en cuenta la relación con el Rol (será muchos a muchos). Un usuario puede tener varios roles

    //FetchType.LAZY: Los roles no se van a cargar al recuperar un usuario; solo se accederá a "usuario.gerRoles()".
    //Con esto se puede evitar el problema N+1 y consultas que son innecesariamente pesadas xd. Después haremos un Fetch explícito con un @EntityGraph

    //No usaremos CascadeType.All. Si borramos un Usuario y tuvieramos eso, se borrarían los roles que son globales xD afectando a otros usuarios
    //La FK en BD ya tiene el ON DELETE CASCADE sobre usuario_roles en la migración V1
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"), //FK hacia Usuario
            inverseJoinColumns = @JoinColumn(name = "rol_id") //FK hacia el rol
    )
    @Builder.Default
    private Set<Rol> roles = new HashSet<>(); //El set evita duplicados y es lo mejor en ManyToMany

    /*
        Por qué Set y no List en ManyToMany

        Semánticamente, los roles de un usuario no están ordenados ni se repiten.

        Hibernate trata Set de forma más eficiente y predecible en este tipo de relación.

        Con List, Hibernate tiende a hacer un DELETE+INSERT de toda la tabla intermedia en cada actualización, lo cual es problemático.
    */




}
