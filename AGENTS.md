# login-jwt

API REST didáctica de autenticación con **JWT de acceso** (firmado HS256 con **JJWT**) y **refresh token opaco**, sobre **Spring Boot** y **MariaDB**. Evolución de `login-tokens`: el access token es autocontenido y la validación es **stateless** — el filtro confía en la firma, sin consultar la base de datos. Detalle funcional, endpoints y ejemplos de uso en `README.md`.

## Rol didáctico

- **DWS**: segunda técnica de la serie de logins, sobre las capas de `login-tokens`.
- Objetivo didáctico: contrastar en clase el token opaco (requiere BD en cada petición) con el JWT (se valida solo con la firma); el refresh sigue siendo opaco y con rotación trazable.
- Punto de partida de `login-security`, que añade el framework de seguridad al mismo flujo.

## Tecnologías

- Java 25 · Spring Boot 4.1.1 (Web MVC, JDBC, Validation)
- MariaDB + Flyway: usuarios y `refresh_tokens` (el access token **no se almacena**)
- `Bcrypt` para contraseñas · JJWT 0.12.6 (api/impl/jackson) para firmar y validar el JWT HS256
- Testcontainers (MariaDB real en los tests de integración)
- Maven Wrapper

## Arquitectura

`controller` / `domain` / `persistence` / `auth`:

- `controller`: endpoints REST, DTOs y manejo de excepciones.
- `domain`: `AuthService`, `UserService`, `JwtService` y los puertos (interfaces de repositorios).
- `persistence`: DAOs JDBC y mapeadores de filas al dominio.
- `auth`: filtro servlet que valida el JWT `Bearer` (firma, emisor y expiración) y publica el contexto de la petición solo con los claims del token.

## Ejecución y pruebas

- MariaDB local (BD `login_jwt`, credenciales `root`/`root` en `application.properties`) y `./mvnw spring-boot:run` → `http://localhost:8080`.
- `./mvnw test` — Testcontainers: requiere Docker en ejecución.
- Usuarios sembrados por Flyway (`demo.user@example.com`, contraseña `password`): didácticos, no reutilizar fuera de desarrollo.

## Configuración

- Firma del JWT: `app.jwt.secret`, configurable con la variable de entorno **`JWT_SECRET`** (mínimo 256 bits; el valor por defecto del repositorio es solo de desarrollo); emisor `app.jwt.issuer`.
- Vida de los tokens: access 15 min (`app.auth.access-token-ttl`), refresh 30 días (`app.auth.refresh-token-ttl`).
- `refresh_tokens` soporta revocación (`revoked_at`) y trazabilidad de rotación (`consumed_at`, `replaced_by_token_id`).

## Estándares

- `docs/base-standards.md`: reglas generales para agentes (idiomas, pasos pequeños, …).
- `docs/backend-standards.md`: estándares específicos del proyecto (capas, mapeadores, flujo de tokens).

## Estado

- ✅ Login con emisión de par de tokens (JWT + refresh opaco), validación de JWT, control de acceso por roles.
- 🚧 Pendiente: endpoint de refresco con rotación y cierre de sesión (revocación). Ver README.

## Repositorio

`git@github.com:cesguiro/login-jwt.git`
