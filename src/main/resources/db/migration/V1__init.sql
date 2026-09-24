-- Resumen: migración inicial. Crea las tablas base del sistema (roles,
-- usuarios, usuario_roles) y siembra los 3 roles iniciales. Flyway
-- ejecuta este archivo una sola vez; su hash queda registrado en la
-- tabla flyway_schema_history. Si el archivo cambia después, Flyway
-- lanzará un error de checksum para evitar drift entre entornos.


-- Tabla: roles
-- Catálogo de roles del sistema. Son globales: no pertenecen a un
-- usuario específico. Los nombres siguen la convención de Spring
-- Security (prefijo "ROLE_") para poder usar hasRole("ADMIN") en
-- las anotaciones de autorización.

CREATE TABLE roles (
                       id      BIGSERIAL    PRIMARY KEY,             -- PK autoincremental (equivalente a IDENTITY en JPA).
                       nombre  VARCHAR(50)  NOT NULL UNIQUE          -- Ej: 'ROLE_ADMIN', 'ROLE_USER'.
);


-- Tabla: usuarios
-- Usuarios registrados en el sistema. La contraseña se guarda como
-- hash BCrypt (≈60 caracteres). Dimensionamos a 255 para tener margen
-- si en el futuro migramos a Argon2 o similar.

CREATE TABLE usuarios (
                          id          BIGSERIAL     PRIMARY KEY,
                          username    VARCHAR(50)   NOT NULL UNIQUE,    -- Se usa para iniciar sesión.
                          email       VARCHAR(100)  NOT NULL UNIQUE,    -- Único para poder usarlo también como identificador.
                          password    VARCHAR(255)  NOT NULL,           -- Hash BCrypt, NUNCA texto plano.
                          nombre      VARCHAR(100)  NOT NULL,           -- Nombre para mostrar en la UI.
                          activo      BOOLEAN       NOT NULL DEFAULT TRUE, -- Permite deshabilitar sin borrar (soft-disable).
                          created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),  -- Auditoría básica.
                          updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- Tabla: usuario_roles
-- Tabla intermedia de la relación ManyToMany entre usuarios y roles.
-- PK compuesta (usuario_id, rol_id) para evitar duplicados.
-- ON DELETE CASCADE en usuario_id: al borrar un usuario, se limpian
-- sus asignaciones. NO se aplica cascade en rol_id hacia roles para
-- no afectar a otros usuarios que compartan el mismo rol.

CREATE TABLE usuario_roles (
                               usuario_id  BIGINT  NOT NULL,
                               rol_id      BIGINT  NOT NULL,
                               PRIMARY KEY (usuario_id, rol_id),
                               CONSTRAINT fk_usuario_roles_usuario
                                   FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
                               CONSTRAINT fk_usuario_roles_rol
                                   FOREIGN KEY (rol_id)     REFERENCES roles(id)
);


-- Seed: roles iniciales del sistema.
-- Los tres roles que la app necesita desde el arranque.
INSERT INTO roles (nombre) VALUES
                               ('ROLE_USER'),
                               ('ROLE_EMPRENDEDOR'),
                               ('ROLE_ADMIN');