package com.milogin.login.domain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity //Marca esta clase como entidad
@Table(name = "roles") //Nombre de la tabla en Postgres
@Getter //Lombok: Genera todos los getters
@Setter //Lombok: Genera todos los setters
@NoArgsConstructor //Lombok: Constructor sin Args (es obligatorio para el JPA)
@AllArgsConstructor //Lombok: Constructor con todos los campos (será util cuando escriba los test, pilas)
@Builder //Lombok: patrón Builder para construcción fluida

public class Rol {

    @Id //Marca este campo como clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) //Delegamos el autoincremento a postgres (como BigSerial)
    private long id;

    @Column (nullable = false, unique = true, length = 50) //La columna no será nula, debe ser único y máximo 50 caracteres
    private String nombre; //Ejemplo: RoleAdmin

    // Nota de diseño: NO defino el lado inverso (Set<Usuario>) aquí.
    // La relación se gestiona desde Usuario. Evitamos ciclos innecesarios
    // en el grafo de objetos y mantenemos esta entidad simple.

}
