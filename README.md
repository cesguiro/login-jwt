# login-jwt

API REST didáctica que implementa autenticación basada en **JWT de acceso y refresh token opaco**, desarrollada con **Spring Boot** y **MariaDB**.

El objetivo del proyecto es servir como ejemplo de referencia sobre cómo gestionar un flujo de autenticación con JWT puro: emisión firmada (HS256 con JJWT), validación stateless mediante filtro y control de acceso por roles extraídos del propio token, sin consultar la base de datos al autenticar.

## Tecnologías

- **Java 25** y **Spring Boot 4.1.1** (Web MVC, JDBC, Validation)
- **MariaDB** como base de datos, con **Flyway** para las migraciones
- **Bcrypt** para el hash de contraseñas
- **JJWT** para la firma y validación del access token JWT (HS256)
- **Testcontainers** para pruebas de integración con base de datos real
- **Maven Wrapper** para la gestión de dependencias y builds

## Arquitectura

El proyecto sigue una arquitectura en capas con separación de responsabilidades:

```
src/main/java/es/cesguiro/login_jwt/
├── controller/    # Controladores REST, DTOs y manejo de excepciones
├── domain/        # Modelos de dominio, servicios y puertos (repositorios)
├── persistence/   # DAOs JDBC, mapeadores e implementaciones de repositorios
└── auth/          # Filtro de autenticación, proveedor JWT y contexto de seguridad
```

- **`controller`**: expone los endpoints REST, valida la entrada y traduce el dominio a DTOs de respuesta.
- **`domain`**: contiene la lógica de negocio (`AuthService`, `UserService`, `JwtService`) y define interfaces de repositorio que la capa de persistencia implementa.
- **`persistence`**: accede a la base de datos mediante DAOs JDBC y mapea filas a objetos de dominio.
- **`auth`**: filtro servlet que extrae el JWT `Bearer` de la cabecera `Authorization`, lo valida (firma, emisor y caducidad) y popula el contexto de seguridad de la petición sin consultar la base de datos. `JwtTokenProvider` implementa el puerto `JwtService` del dominio.

## Endpoints

| Método | Ruta            | Descripción                              | Acceso                     |
|--------|-----------------|------------------------------------------|----------------------------|
| POST   | `/auth/login`   | Autentica al usuario y devuelve un par de tokens (access + refresh) | Pública |
| GET    | `/auth/me`      | Devuelve los datos del usuario autenticado | Token de acceso requerido |
| GET    | `/demo/public`  | Ruta de ejemplo sin autenticación        | Pública                    |
| GET    | `/demo/profile` | Devuelve el id y los roles del usuario autenticado | Token de acceso requerido |
| GET    | `/demo/admin`   | Ruta de ejemplo reservada                | Requiere rol `ROLE_ADMIN`  |

### Ejemplo de uso

```bash
# Iniciar sesión
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "demo.user@example.com", "password": "password"}'

# Acceder a una ruta protegida con el token recibido
curl http://localhost:8080/demo/profile \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Modelo de tokens

- El **access token** es un **JWT firmado con HS256**: contiene `iss`, `sub` (id de usuario), `roles`, `iat` y `exp`. Es autocontenido y **no se almacena** en la base de datos.
- La autenticación es **stateless**: el filtro construye el principal solo con los claims del JWT, sin consultar la base de datos. (`GET /auth/me` sí carga el usuario desde la base de datos para devolver el perfil completo; es la excepción que sirve para contrastar ambos enfoques en clase.)
- El **refresh token** sigue siendo **opaco**, generado criptográficamente y almacenado en base de datos como **hash SHA-256** (`BINARY(32)`), nunca en claro.
- El **access token** tiene una vida útil por defecto de **15 minutos** (`app.auth.access-token-ttl`).
- El **refresh token** tiene una vida útil por defecto de **30 días** (`app.auth.refresh-token-ttl`).
- La tabla `refresh_tokens` soporta revocación (`revoked_at`) y trazabilidad de rotación (`consumed_at`, `replaced_by_token_id`).
- La firma usa la clave `app.jwt.secret` (mínimo 256 bits, configurable con `JWT_SECRET`) y el emisor `app.jwt.issuer`.

## Advertencias del JWT puro (para los apuntes de 2DAW)

- Los **roles quedan congelados** en el token hasta que caduca: si un usuario pierde `ROLE_ADMIN`, seguirá entrando a `/demo/admin` con su JWT vigente.
- **Deshabilitar un usuario no lo expulsa**: un JWT emitido antes de `enabled = 0` sigue aceptado en `/demo/profile` hasta su `exp`. Hay un test de integración que lo demuestra (`demoProfileStillAcceptsTokenAfterUserIsDisabled`).
- El **access token no se puede revocar**: no hay denylist; solo cabe esperar a que caduque. Por eso el TTL debe ser corto (5-15 min) y el refresh token es el que permite cerrar sesión o rotar.
- **No guardar datos sensibles** en los claims: el payload solo va firmado, no cifrado, y cualquiera puede leerlo con Base64.
- Validar siempre **firma, `iss` y `exp`**. Aceptar `alg=none` o no comprobar el emisor permite forjar tokens.
- La **rotación del secreto invalida todos los tokens** a la vez; prevéd rotación planificada y TTL corto para mitigarlo.
- Cuidado con el **skew de reloj** entre servidores al comprobar `iat`/`exp`.

> ⚠️ Los usuarios sembrados por las migraciones son **didácticos** y su contraseña es `password`. No reutilizar esta configuración en un entorno real.

## Usuarios de demostración

Las migraciones crean dos usuarios de ejemplo (contraseña: `password`):

| Email                       | Roles                    |
|-----------------------------|--------------------------|
| `demo.admin@example.com`    | `ROLE_ADMIN`, `ROLE_USER` |
| `demo.user@example.com`     | `ROLE_USER`              |

## Requisitos previos

- **JDK 25**
- **Docker** (necesario para ejecutar los tests de integración con Testcontainers)
- **MariaDB** en ejecución local (para arrancar la aplicación)

## Puesta en marcha

1. Crear la base de datos (Flyway creará las tablas automáticamente al arrancar):

   ```sql
   CREATE DATABASE login_jwt;
   ```

2. Ajustar las credenciales de acceso en `src/main/resources/application.properties` si no coinciden con las locales (`root` / `root` por defecto). Definir `JWT_SECRET` en producción con un valor Base64 de al menos 32 bytes.

3. Arrancar la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

4. La API quedará disponible en `http://localhost:8080`.

## Tests

Los tests de integración levantan contenedores MariaDB mediante Testcontainers, por lo que requieren Docker en ejecución:

```bash
./mvnw test
```

## Estado del proyecto

- ✅ Login con emisión de par de tokens (JWT + refresh opaco)
- ✅ Validación de JWT mediante filtro
- ✅ Control de acceso por roles en rutas de demostración
- 🚧 Endpoint de refresco de tokens (rotación de refresh token)
- 🚧 Endpoint de cierre de sesión (revocación de tokens)
