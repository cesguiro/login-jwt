---
description: Estándares de desarrollo de backend, buenas prácticas y convenciones para la aplicación Spring Boot.
globs: ["src/main/java/**/*.java", "src/test/java/**/*.java", "pom.xml", "build.gradle"]
alwaysApply: false
---

# Estándares del proyecto de backend

Proyecto educativo de autenticación con JWT.

## Índice
1. [Objetivo y tecnologías](#1-objetivo-y-tecnologías)
2. [Arquitectura de tres capas](#2-arquitectura-de-tres-capas)
3. [Modelos y mapeadores](#3-modelos-y-mapeadores)
4. [Usuarios y roles](#4-usuarios-y-roles)
5. [Autenticación con JWT](#5-autenticación-con-jwt)
6. [Persistencia y migraciones](#6-persistencia-y-migraciones)
7. [Pruebas](#7-pruebas)
8. [Errores y observabilidad](#8-errores-y-observabilidad)

## 1. Objetivo y tecnologías

- El proyecto enseña el funcionamiento de un sistema de autenticación con JWT antes de estudiar Spring Security.
- Usar Spring Boot y Spring MVC para la API.
- Usar **Spring JDBC básico** (`JdbcTemplate` y mecanismos equivalentes de Spring JDBC) para el acceso a datos. No introducir JPA, Hibernate ni Spring Data.
- No usar Spring Security. La autenticación JWT se implementa explícitamente con JJWT para que el flujo sea visible y comprensible.
- Usar Flyway para versionar el esquema y Testcontainers con **MariaDB 12.3**, la misma versión que la base de datos real.
- Mantener el código de producción en inglés; la documentación, comentarios explicativos y mensajes destinados a usuarios en español, conforme a `base-standards.md`.

## 2. Arquitectura de tres capas

Mantener las dependencias en una sola dirección:

```text
controller ─┐
auth ────────┴──> domain <── persistence
```

`controller`, `auth` y `persistence` dependen de las abstracciones del dominio; `domain` no depende de Spring MVC, JDBC ni de detalles de persistencia.

### 2.1 `controller`

Contiene la interfaz HTTP:

- Controladores en el paquete raíz `controller`, por ejemplo `UserController` y `AuthController`.
- DTOs separados de request y response en `controller.dto`.
- Manejadores de excepciones HTTP en `controller.exception`, por ejemplo un `@ControllerAdvice` global.
- Mapeadores entre los DTOs y los modelos del dominio en `controller.mapper`.

Los controladores validan y traducen la entrada HTTP, invocan servicios del dominio y construyen la respuesta. No contienen SQL, reglas de negocio ni lógica de generación o renovación de tokens.

### 2.2 `domain`

Contiene las reglas de negocio y sus contratos:

- `domain.model`: modelos del dominio, como `User`, `Role` y el principal autenticado.
- `domain.service`: contratos de servicios, como `UserService` y `AuthService`.
- `domain.service.impl`: implementaciones, como `UserServiceImpl` y `AuthServiceImpl`.
- `domain.repository`: interfaces de repositorios, por ejemplo `UserRepository` y `TokenRepository`.
- `domain.exception`: excepciones propias del dominio.

Los servicios coordinan los casos de uso y dependen de interfaces de repositorio, nunca de DAOs JDBC concretos. Usar nombres claros y específicos; separar autenticación (`AuthService`) de operaciones de usuario (`UserService`) cuando tengan responsabilidades distintas.

Los límites transaccionales se definen en los servicios que coordinan cada caso de uso. Los repositorios realizan operaciones de persistencia, pero no abren sus propias transacciones.

### 2.3 `persistence`

Contiene los adaptadores de almacenamiento:

- `persistence.repository.impl`: implementaciones de las interfaces de `domain.repository`.
- `persistence.dao`: contratos de acceso a datos.
- `persistence.dao.jdbc`: implementaciones JDBC de los DAOs.
- `persistence.mapper`: mapeadores que convierten una fila de `ResultSet` en un modelo o valor del dominio.

Los DAOs encapsulan consultas SQL y el acceso a `JdbcTemplate`; cada consulta SQL se declara como variable local en el método que la ejecuta. Los repositorios traducen las operaciones del dominio a llamadas a los DAOs y pueden combinar resultados de varias consultas. Para el login, cargar el usuario y sus roles por separado mantiene visible el flujo; `JdbcTemplate` aplica el `RowMapper` a cada fila devuelta, sin necesitar mapeadores especiales para colecciones. No colocar SQL en controladores o servicios, ni exponer DAOs directamente a los controladores.

### 2.4 `auth`

Contiene los adaptadores HTTP propios del flujo de autenticación, separados de los controladores y DTOs específicos de cada recurso:

- `auth.filter`: filtros Servlet, como el que lee el token de acceso de la cabecera `Authorization`.

El filtro puede depender de Spring MVC/Servlet y de los contratos del dominio, pero el dominio no debe depender del filtro ni de detalles HTTP. Su responsabilidad es extraer y validar el token y establecer el principal autenticado para la petición; las reglas de negocio de autenticación permanecen en el dominio.

## 3. Modelos y mapeadores

Mantener separados los modelos del dominio, los DTOs HTTP y los datos de persistencia. No devolver modelos del dominio directamente desde la API ni usar DTOs como modelos de persistencia.

Usar estas direcciones de mapeo:

```text
ResultSet -> modelo del dominio
DTO de request -> modelo/entrada del dominio
modelo del dominio -> DTO de response
```

Los mapeadores deben tener una responsabilidad clara y ser sencillos de probar. Evitar lógica de negocio en ellos. Los DTOs de response no deben incluir contraseñas, hashes, tokens internos ni datos de persistencia que el cliente no necesite.

## 4. Usuarios y roles

- Un usuario puede tener **varios roles** simultáneamente.
- Modelar la relación como muchos-a-muchos, con tablas para usuarios y roles y una tabla de unión. Flyway debe definir las claves foráneas y restricciones necesarias para impedir relaciones duplicadas.
- Los roles iniciales son `ROLE_ADMIN` y `ROLE_USER`. Mantener el prefijo `ROLE_` de forma consistente en base de datos, modelos, principal y comprobaciones de autorización.
- Representar los roles de un usuario como una colección sin duplicados, por ejemplo un `Set<Role>` o `Set<String>`.
- El principal autenticado debe contener únicamente la identidad y las autoridades necesarias (por ejemplo, id de usuario y roles); no debe exponer el modelo completo ni credenciales.
- En JWT puro la identidad y los roles vienen de los claims del token, sin consultar la base de datos al autenticar. Solo `GET /auth/me` carga el usuario completo desde la persistencia para devolver el perfil; sirve en clase para contrastar stateless frente a stateful.

## 5. Autenticación con JWT puro

- El access token es un JWT firmado con HS256 y contiene `iss`, `sub`, `roles`, `iat` y `exp`. El refresh token sigue siendo una cadena opaca y aleatoria.
- Enviar el JWT de acceso en `Authorization: Bearer <token>`. No aceptar tokens de autenticación en query parameters ni registrarlos en logs.
- Distinguir claramente el access token JWT del refresh token opaco, con expiraciones y estados independientes.
- El filtro de autenticación debe extraer el JWT, validar firma, emisor y caducidad, y construir el principal autenticado solo con los claims, sin consultar la base de datos. Las rutas públicas (por ejemplo, login y renovación) deben quedar excluidas según corresponda.
- El refresh token es rotativo: al renovarlo, invalidar el anterior y emitir uno nuevo. La invalidación y la creación deben formar una operación atómica para evitar que el mismo refresh token produzca varias renovaciones concurrentes válidas.
- En cada autenticación solo se comprueban firma, emisor y caducidad. Rechazar tokens inválidos o caducados sin revelar información sensible.
- Generar el refresh token con un generador criptográficamente seguro. Almacenar un hash criptográfico del refresh token en la base de datos en lugar del valor reutilizable en claro; nunca almacenar tokens en logs. No guardar datos sensibles en los claims del JWT (van firmados, no cifrados).
- Mantener los tiempos de expiración y la configuración JWT (`app.auth.*`, `app.jwt.secret`, `app.jwt.issuer`) en configuración, no dispersos como valores mágicos en el código. La clave debe tener al menos 256 bits y externalizarse con `JWT_SECRET` en producción. Usar TTL corto para el access token (5-15 min).
- Comprobar autorización por roles después de autenticar; autenticación (quién es el usuario) y autorización (qué puede hacer) son pasos distintos.

### Limitaciones didácticas del JWT puro (para explicar en clase)

- Los roles quedan congelados hasta `exp`; deshabilitar un usuario o quitarle un rol no invalida sus JWT vigentes.
- El access token no es revocable sin una denylist, que rompería el carácter stateless.
- La rotación del secreto invalida todos los tokens; el skew de reloj afecta a `iat`/`exp`.
- Los tests `authenticateStillAcceptsTokenWhenUserIsDisabledInDatabase` y `demoProfileStillAcceptsTokenAfterUserIsDisabled` demuestran estas limitaciones a propósito.

La implementación debe permanecer explícita y pequeña para servir de material didáctico. No crear una abstracción genérica de seguridad que oculte cómo se lee la cabecera, se valida el token y se obtiene el usuario.

## 6. Persistencia y migraciones

- Todas las modificaciones del esquema se hacen con migraciones Flyway en `src/main/resources/db/migration` y con nombres versionados descriptivos, por ejemplo `V1__create_users_and_roles.sql`.
- No depender de la generación automática del esquema por ORM. No modificar una migración que ya se haya aplicado en una base de datos compartida; añadir una migración nueva.
- Escribir SQL compatible con MariaDB 12.3 y probarlo contra esa versión real mediante Testcontainers; H2 no sustituye estas pruebas.
- Definir explícitamente claves primarias, claves foráneas, restricciones únicas e índices necesarios para las consultas de usuarios, roles y tokens.
- Mantener las operaciones que deben ser indivisibles —en particular, rotación de refresh tokens— dentro de una transacción.
- Externalizar credenciales y configuración de entorno; no incluir secretos de producción en el repositorio.

## 7. Pruebas

- Seguir TDD como indica `base-standards.md`: escribir pruebas del comportamiento esperado antes de implementar una funcionalidad.
- Las pruebas de integración de persistencia deben arrancar MariaDB 12.3 con Testcontainers y ejecutar las migraciones Flyway. No simular MariaDB con H2.
- Probar DAOs y repositorios contra la base de datos real del contenedor, incluyendo relaciones de varios roles por usuario y restricciones relevantes.
- Probar los servicios para los casos de uso y sus límites: credenciales válidas e inválidas, token ausente/desconocido/caducado/revocado, renovación y reutilización de un refresh token ya rotado, y autorización con distintos roles.
- Probar el filtro HTTP con cabeceras Bearer válidas e inválidas, rutas públicas y protegidas, y la identidad/roles expuestos al controlador.
- Mantener nombres de clases y métodos de prueba en inglés. Cada prueba debe expresar un comportamiento observable, no detalles internos de implementación.
- Documentar cómo ejecutar las pruebas y los requisitos locales de Docker/Testcontainers.

## 8. Errores y observabilidad

- Traducir excepciones del dominio a respuestas HTTP en el manejador global de `controller`; evitar repetir traducciones en cada controlador.
- No enviar stack traces, SQL, contraseñas, tokens ni detalles internos en las respuestas HTTP o en logs.
- Usar mensajes y logs proyectados en español, sin incluir datos secretos. Registrar información suficiente para diagnosticar errores sin revelar credenciales.
