# Defensa de Proyecto — `ms-usuarios-boot`

Microservicio de autenticación y gestión de usuarios de la **Plataforma de Arrendamientos CR**, reescrito desde el servicio original en Node/TypeScript a **Spring Boot 3.3 sobre Java 21 LTS** con arquitectura hexagonal estricta.

> Documento de defensa: explica **qué se construyó, por qué se construyó así, qué evidencia de calidad respalda el trabajo y qué decisiones de diseño se tomaron**.

---

## 1. Contexto y motivación

El proyecto original cuenta con un servicio `MS-Usuarios-Plataforma-Arrendamientos-CR` implementado en **Node.js 20 + TypeScript + Express** que expone autenticación, registro, OAuth con Google y perfil de usuario. La plataforma completa está compuesta por varios microservicios (usuarios, propiedades, contratos, pagos, mensajes, notificaciones) y un frontend en **Azure Static Web Apps**.

La decisión de reescribir este servicio en **Spring Boot 3.3 + Java 21 LTS** responde a cuatro objetivos:

| Objetivo | Razón |
|---|---|
| **Consistencia con la plataforma** | Los demás microservicios (contratos, pagos, mensajes, notificaciones) están en el ecosistema JVM/.NET empresarial. Tener el core de autenticación en una tecnología distinta introduce fricción operativa, de monitorización y de contratación. |
| **Madurez del ecosistema** | Spring Security 6, Spring Data JPA, Flyway, Micrometer y Springdoc son librerías de referencia industrial con integración nativa, soporte a largo plazo y documentación extensa. |
| **Tipado fuerte en tiempo de compilación** | Los records de Java 21, el sistema de tipos de JPA/Hibernate y la validación con Jakarta Bean Validation eliminan categorías enteras de bugs que en JS aparecen en runtime. |
| **Tooling empresarial** | Soporte de primer nivel en Azure (App Service, Application Insights, Monitor), integración con Key Vault y compatibilidad con el ciclo de vida corporativo. |

La migración se realizó siguiendo el patrón **strangler fig**: el nuevo servicio convive con el de Node mientras se valida la paridad funcional, y el corte de tráfico se hace de forma gradual en Azure API Management (ver `docs/CUTOVER_RUNBOOK.md`).

---

## 2. Stack y decisiones de plataforma

| Componente | Tecnología | Justificación |
|---|---|---|
| Runtime | **Java 21 LTS** | Records, pattern matching, text blocks, virtual threads disponibles. LTS garantiza soporte hasta 2031. |
| Framework | **Spring Boot 3.3.4** | Línea estable con soporte a Jakarta EE 10. |
| Build | **Maven 3.9** | Estándar corporativo, integración simple con `spring-boot-maven-plugin`. |
| Persistencia | **Spring Data JPA + Hibernate 6 + mssql-jdbc** | Compatibilidad nativa con Azure SQL Database. |
| Migraciones | **Flyway 10** (V1–V5) | Versionado de esquema, despliegues reproducibles. |
| Validación | **Jakarta Bean Validation** | Anotaciones declarativas en DTOs. |
| JWT | **io.jsonwebtoken:jjwt 0.12.6** | API moderna, soporte de algoritmos HS256/HS384/HS512 y RS256. |
| Google OAuth | **google-api-client 2.8.0** (`GoogleIdTokenVerifier`) | Verificación de `id_token` contra los endpoints oficiales de Google. |
| GitHub OAuth | **WebClient** + `HttpClient` de Java 21 | Authorization Code flow contra `https://github.com/login/oauth/`. |
| Rate limit | **Bucket4j 8.14.0** | Algoritmo token-bucket en memoria, sin dependencias externas. |
| OpenAPI | **springdoc-openapi 2.6.0** | Genera spec OpenAPI 3 desde las anotaciones de los controllers. |
| Seguridad | **Spring Security 6 + BCrypt 10** | Filtros custom en cadena, sesión stateless. |
| Observabilidad | **Micrometer Prometheus + Azure Application Insights** | Métricas + trazas en el mismo pipeline. |
| Integración | **Testcontainers 1.20.3 + WireMock 3.9.1** | Pruebas contra MS SQL real y mocks de Google OAuth. |
| Contract tests | **Newman** (Postman CLI) | Validación de paridad HTTP entre Node y Spring. |
| Tests | **JUnit 5 + Mockito + AssertJ + H2 (test)** | Cobertura por capas. |

> **Decisión de Java 21 vs 17**: se eligió 21 LTS para aprovechar records como value objects del dominio, pattern matching en validaciones y `HttpClient` nativo en lugar de OkHttp/Retrofit. El rendimiento frente a 17 LTS es equivalente; el beneficio es de expresividad.

---

## 3. Arquitectura hexagonal (Ports & Adapters)

El código se organiza en **tres anillos concéntricos** con la **regla de dependencia** que apunta siempre hacia el centro:

```
┌────────────────────────────────────────────────────────────────┐
│  infrastructure/   (Adaptadores: JPA, JWT, OAuth, Web, Filtros)│
│   ┌──────────────────────────────────────────────────────────┐  │
│   │  application/   (Servicios: orquestación de use cases)  │  │
│   │   ┌────────────────────────────────────────────────────┐ │  │
│   │   │  domain/   (Modelo, excepciones, ports) — NÚCLEO  │ │  │
│   │   │                                                    │ │  │
│   │   │   • Sin Spring, sin JPA, sin Servlet API.          │ │  │
│   │   │   • Tests unitarios con JUnit puro.                 │ │  │
│   │   │   • Compila en milisegundos.                       │ │  │
│   │   └────────────────────────────────────────────────────┘ │  │
│   └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### Estructura real

```
src/main/java/com/arrendamientos/usuarios/
├── domain/                            # NÚCLEO — sin dependencias de framework
│   ├── model/                         # Usuario, UsuarioId, Correo, PasswordHash,
│   │                                  # RolUsuario, GoogleUserInfo, GitHubUserInfo,
│   │                                  # UsuarioView, Propiedad
│   ├── exception/                     # DomainException + 8 excepciones específicas
│   └── port/
│       ├── in/                        # 12 use cases (interfaces)
│       └── out/                       # 7 ports (UsuarioRepositoryPort, TokenProviderPort,
│                                      #  PasswordEncoderPort, GoogleTokenVerifierPort, …)
│
├── application/                       # Orquestación — depende solo de `domain`
│   ├── service/                       # UsuarioService (10 use cases),
│   │                                  # EmailVerificationService,
│   │                                  # CatalogoPropiedadesService (mock)
│   └── dto/                           # Commands y AuthResult
│
└── infrastructure/                    # ADAPTADORES — depende de Spring, JPA, etc.
    ├── config/                        # AppProperties, SecurityConfig, OpenApiConfig, AuthMetrics
    ├── persistence/                   # Entidades JPA, repos, mappers, adapters
    ├── security/                      # JwtAuthenticationFilter, JwtTokenProviderAdapter,
    │                                  # BcryptPasswordEncoderAdapter, AuthenticatedUser
    ├── web/                           # AuthController, UsuarioController, PropiedadesController,
    │                                  # HealthController, GlobalExceptionHandler
    ├── web/filter/                    # ApimHeaderFilter (sub-key APIM + mTLS)
    ├── ratelimit/                     # RateLimitFilter (Bucket4j, 4 categorías)
    ├── google/                        # GoogleTokenVerifierAdapter
    ├── github/                        # GitHubTokenVerifierAdapter
    └── email/                         # LoggingEmailSenderAdapter
```

### Inversión de dependencias en la práctica

El `domain` define lo que necesita:

```java
// domain/port/out/UsuarioRepositoryPort.java
public interface UsuarioRepositoryPort {
    Optional<Usuario> porId(String id);
    Optional<Usuario> porCorreo(String correo);
    Optional<Usuario> porGoogleId(String googleId);
    Optional<Usuario> porGitHubId(Long githubId);
    Usuario guardar(Usuario u);
    boolean eliminar(String id);
    int incrementarIntentosFallidos(String id);
    void resetearIntentosFallidos(String id);
    // ...
}
```

`application` lo consume a través de la interfaz, sin saber que existe JPA:

```java
// application/service/UsuarioService.java
@Override @Transactional
public AuthResult login(LoginCommand cmd) {
    Usuario usuario = usuarios.porCorreo(cmd.correo())
            .orElseThrow(CredencialesInvalidasException::new);
    // ...
}
```

`infrastructure` lo implementa con JPA + SQL Server:

```java
// infrastructure/persistence/adapter/UsuarioRepositoryAdapter.java
@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {
    private final UsuarioJpaRepository jpa;
    private final UsuarioEntityMapper mapper;
    @Override
    public Optional<Usuario> porId(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }
    // ...
}
```

**Beneficios concretos que aporta este diseño:**

1. **El dominio es testeable en milisegundos**, sin levantar el contexto de Spring. La suite `domain.*Test` (27 tests) corre sin `@SpringBootTest`.
2. **El servicio se prueba con puertos mockeados** (`application.UsuarioServiceTest`, 37 tests) sin tocar base de datos ni HTTP.
3. **Cambiar de SQL Server a PostgreSQL o a DynamoDB** implica escribir un nuevo adapter; `domain` y `application` no se tocan.
4. **Reemplazar Google OAuth por un IdP corporativo** (Azure AD, Okta, Auth0) es un cambio aislado en `infrastructure/google/` o un nuevo paquete `infrastructure/azuread/`.

---

## 4. Modelo de dominio

El agregado principal es **`Usuario`**, modelado como un **record inmutable** de Java 21:

```java
public record Usuario(
        UsuarioId id,
        String nombre,
        String correo,
        PasswordHash contrasenaHash,
        RolUsuario rol,
        String telefono,
        String avatar,
        String googleId,
        Long gitHubId,
        Instant fechaRegistro,
        Instant ultimoLogin,
        int intentosFallidos,
        Instant bloqueadoHasta
) {
    public boolean esCuentaBloqueada(Instant ahora) { /* ... */ }
    public boolean esOAuth() { return contrasenaHash == null; }
    public boolean puedeActualizarseComo(String idEditor) { /* ... */ }
    public UsuarioView aView() { /* proyección segura hacia afuera */ }
}
```

### Value Objects con invariantes

Los tipos `UsuarioId`, `Correo`, `PasswordHash` y `RolUsuario` **encapsulan reglas de validación en su constructor** y lanzan `IllegalArgumentException` si se viola una invariante. Esto desplaza la validación del controller al modelo y garantiza que un `Usuario` mal formado no pueda existir en memoria.

```java
public record Correo(String value) {
    public Correo {
        if (value == null || !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Correo inválido: " + value);
        }
    }
}
```

### Excepciones de dominio

Diez tipos específicos en `domain/exception/`, todos heredando de `DomainException`. La capa web las mapea a HTTP mediante `GlobalExceptionHandler`:

| Excepción | HTTP |
|---|---|
| `CredencialesInvalidasException` | 401 |
| `CuentaBloqueadaException` | 429 |
| `CorreoYaRegistradoException` | 409 |
| `UsuarioNoEncontradoException` | 404 |
| `PermisoDenegadoException` | 403 |
| `CuentaGoogleVinculadaException` | 409 |
| `CuentaGitHubVinculadaException` | 409 |
| `ValidacionException` | 400 |
| `TokenInvalidoException` | 401 |
| `DomainException` (base) | 500 |

---

## 5. Seguridad — el corazón del servicio

Un servicio de autenticación vive o muere por su seguridad. Se implementó **defensa en profundidad** con cuatro capas independientes.

### 5.1 Autenticación: tres vías

| Vía | Endpoint | Mecanismo |
|---|---|---|
| **Email + contraseña** | `POST /api/auth/login` | BCrypt strength 10 + comparación timing-safe (`passwordEncoder.matches`). |
| **Google OAuth** | `POST /api/auth/google` | `GoogleIdTokenVerifier` valida firma, `iss`, `aud`, `email_verified`, `hd` (hosted domain) y `nonce` (anti-replay). |
| **GitHub OAuth** | `POST /api/auth/github` | Authorization Code flow: se intercambia `code` por `access_token` contra `https://github.com/login/oauth/access_token`, luego se llama a `https://api.github.com/user` y `https://api.github.com/user/emails`. |

Todas las vías producen un **`AuthResult`** con la misma forma: `accessToken` (24h), `refreshToken` (7d) y `UsuarioView`. Esto simplifica el frontend: un único contrato de respuesta.

### 5.2 Autorización: JWT con revocación

El servicio emite **JWT HS384** con claims estándar y custom:

```json
{
  "jti":  "b40efb01-fab0-41ea-88b7-aa93f3b21ad3",
  "sub":  "usr-083",
  "id":   "usr-083",
  "correo": "maria.rodriguez@arrendamientoscr.com",
  "rol":  "dueno",
  "iat":  1780939451,
  "exp":  1781025851
}
```

- **Algoritmo:** HS384 (probado contra los demás microservicios de la plataforma que usan HS256/HS384; documentado en `COORDINACION_MICROSERVICIOS.md`).
- **Clave:** `JWT_SECRET` cargado desde variable de entorno (mín. 32 bytes, obligatorio al arranque).
- **Refresh tokens** llevan `tipo: "refresh"` y se revocan por JTI al usarlos (rotación).
- **Logout real** persiste el `jti` en la tabla `tokens_revocados` con su `exp`. `JwtAuthenticationFilter` consulta esa tabla en cada request y rechaza tokens revocados — ver `JwtAuthenticationFilter.java:53-79`.
- **Verificación de ownership**: las operaciones de update/delete sobre `/api/usuario/{id}` comparan `id` del path con `id` del JWT y lanzan `PermisoDenegadoException` (403) si no coinciden.

### 5.3 Lockout contra fuerza bruta

```
intentos fallidos ≥ 5  →  cuenta bloqueada 15 minutos
```

La columna `intentos_fallidos` y `bloqueado_hasta` viven en la tabla `Usuarios` (migración `V3__lockout_and_revoked_tokens.sql`). El bloqueo sobrevive a reinicios del servicio porque está en BD, no en memoria.

### 5.4 Cadena de filtros (orden importa)

`SecurityConfig.java:84-86` declara explícitamente el orden:

```
RateLimitFilter  →  ApimHeaderFilter  →  JwtAuthenticationFilter  →  Controller
       (antes de UsernamePasswordAuthenticationFilter)
```

| Filtro | Función |
|---|---|
| **RateLimitFilter** | Token-bucket por IP+usuario en 4 categorías: `auth` (5/15min), `read` (200/15min), `write` (50/15min), `general` (100/15min). Bypasea `/api/health` y `/actuator/*`. Configurable vía `RL_*` env vars. |
| **ApimHeaderFilter** | Exige `Ocp-Apim-Subscription-Key` en producción. Opcionalmente valida el thumbprint SHA-1 del certificado cliente en `X-ARR-ClientCert` (mTLS). En dev/test, bypass si `APIM_VALIDATE_CLIENT_CERT=false`. |
| **JwtAuthenticationFilter** | Extrae el `Bearer` token, valida firma, expiración, revocación y claims mínimos (`id`, `rol`). Crea un `AuthenticatedUser` en el `SecurityContext` con `ROLE_DUENO` o `ROLE_INQUILINO`. |

### 5.5 Mitigación de OWASP Top 10

| Riesgo | Mitigación |
|---|---|
| **A01 Broken Access Control** | Ownership check + `@AuthenticationPrincipal` en controllers + 403 explícito del `AccessDeniedHandler`. |
| **A02 Cryptographic Failures** | BCrypt 10 para passwords, JWT HS384 con secreto ≥ 32 bytes, conexión a SQL Server con `encrypt=true`. |
| **A03 Injection** | JPA con parámetros preparados (`@Param`, no concatenación), Jackson con `default-property-inclusion: non_null`. |
| **A04 Insecure Design** | Arquitectura hexagonal, lockout, rate limit, tokens revocados, validaciones en DTOs con `@Valid`. |
| **A05 Security Misconfig** | CORS explícito por perfil, CSRF deshabilitado solo por ser stateless JWT, headers de error sin stack traces en prod. |
| **A07 Identification & Auth Failures** | Rate limit + lockout + JWT con expiración corta + refresh tokens rotados + revocación. |
| **A09 Logging Failures** | SLF4J + Logback con patrón estructurado, agent de Application Insights para trazas. |

---

## 6. Persistencia y migraciones

### Stack

- **Spring Data JPA + Hibernate 6** contra **Azure SQL Database** (driver `mssql-jdbc`).
- **HikariCP** como pool de conexiones (10 máx, validación 5s, vida máxima 30min).
- **Flyway 10** para versionado de esquema, con `baseline-on-migrate: true` para entornos existentes.
- **`ddl-auto: none`** — Hibernate nunca modifica el esquema; todo cambio es una migración.

### Cinco migraciones

| Versión | Archivo | Propósito |
|---|---|---|
| V1 | `V1__usuarios.sql` | Tabla `Usuarios` con `Id NVARCHAR(50)`, `Correo UNIQUE`, índice en `GoogleId`. |
| V2 | `V2__sequences.sql` | Secuencia `seq_usuarios` para IDs legibles tipo `usr-001`. |
| V3 | `V3__lockout_and_revoked_tokens.sql` | Columnas `intentos_fallidos`, `bloqueado_hasta` + tabla `tokens_revocados`. |
| V4 | `V4__add_github_id_to_usuarios.sql` | Columna `git_hub_id BIGINT NULL`. |
| V5 | `V5__add_github_id_index.sql` | Índice único parcial en `git_hub_id` (solo no-NULL). |

### Decisión: IDs legibles vs GUIDs

El frontend recibe IDs tipo `usr-083`, `prop-024`, `inv-123` en vez de GUIDs. Esto se decidió por **UX (los IDs se muestran en URLs y logs)** y por **compatibilidad con el servicio Node original**. El `SequenceGeneratorPort` abstrae la generación; en SQL Server se hace con `NEXT VALUE FOR seq_usuarios`, en tests con un mock que devuelve `usr-test-N`.

> Trade-off conocido: los IDs son predecibles. Esto se mitiga porque la API exige JWT en cada endpoint que los usa y porque no se exponen como claves de seguridad, solo como referencia.

---

## 7. API REST

Todos los endpoints devuelven JSON, usan UTF-8 y propagan el `traceId` de Application Insights en `correlation-id` cuando está disponible.

### 7.1 Endpoints públicos (sin JWT)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Iniciar sesión con email + contraseña. |
| `POST` | `/api/auth/registro` | Registrar un nuevo usuario. |
| `POST` | `/api/auth/google` | Login/registro con Google OAuth. |
| `POST` | `/api/auth/github` | Login/registro con GitHub OAuth (Authorization Code). |
| `GET`  | `/api/auth/verify-email/{token}` | Verificar email. |
| `GET`  | `/api/propiedades` | Listar propiedades (mock). |
| `GET`  | `/api/propiedades/{id}` | Detalle de propiedad (mock). |
| `GET`  | `/api/health` | Health check con verificación de BD. |

### 7.2 Endpoints autenticados (JWT Bearer)

| Método | Ruta | Descripción |
|---|---|---|
| `GET`   | `/api/auth/profile` | Perfil del usuario autenticado. |
| `POST`  | `/api/auth/refresh` | Refrescar access token (rota el refresh). |
| `POST`  | `/api/auth/logout` | Revocar el token actual. |
| `POST`  | `/api/auth/send-verification-email` | Reenviar email de verificación. |
| `GET`   | `/api/usuarios` | Listar usuarios (paginado). |
| `GET`   | `/api/usuario/{id}` | Obtener un usuario. |
| `PUT`   | `/api/usuario/{id}` | Actualizar (solo el propio usuario). |
| `DELETE`| `/api/usuario/{id}` | Eliminar (solo el propio usuario). |

### 7.3 Documentación y operación

| Ruta | Propósito |
|---|---|
| `GET /v3/api-docs` | Spec OpenAPI en JSON. |
| `GET /swagger-ui.html` | UI interactiva de Swagger. |
| `GET /actuator/health` | Health con detalle de BD. |
| `GET /actuator/prometheus` | Métricas en formato Prometheus. |
| `GET /actuator/info` | Metadata de la app. |

> Los paths de OpenAPI/Swagger se exponen bajo `/api/...` para que el proxy del **Azure Static Web Apps** (`/api/*` → App Service) los pueda alcanzar. Sin esto, el navigation fallback del SWA devuelve el `index.html` de la SPA en vez del spec.

### 7.4 Códigos de error

`GlobalExceptionHandler` mapea todas las excepciones a respuestas JSON consistentes:

```json
{ "error": "Unauthorized", "message": "Token inválido o expirado" }
```

Status codes usados: 200, 201, 400, 401, 403, 404, 409, 422, 429, 500.

---

## 8. Observabilidad

### 8.1 Métricas de negocio (9 counters)

Definidas en `infrastructure/config/AuthMetrics.java` y registradas en `MeterRegistry` (Micrometer). Quedan expuestas en `/actuator/prometheus`:

| Counter | Cuándo se incrementa |
|---|---|
| `auth.login.success` | Login email+password exitoso. |
| `auth.login.failure` | Credenciales inválidas. |
| `auth.register.success` | Registro exitoso. |
| `auth.register.conflict` | Correo duplicado. |
| `auth.google.success` / `.failure` | Login Google. |
| `auth.github.success` / `.failure` | Login GitHub. |
| `auth.account.locked` | Cuenta bloqueada por 5 intentos. |
| `auth.token.refresh` | Refresh ejecutado. |
| `auth.logout` | Logout con revocación. |

### 8.2 Métricas de plataforma

- **HTTP**: latencia P50/P90/P95/P99 por endpoint, conteo de respuestas por status code.
- **JVM**: heap por generación, GC pauses, threads activas, clases cargadas.
- **HikariCP**: conexiones activas, conexiones en espera, tiempo de espera promedio.
- **Proceso**: CPU, memoria residente, file descriptors.

### 8.3 Azure Application Insights

Se activa con el profile `azure-insights` y la variable `APPLICATIONINSIGHTS_CONNECTION_STRING`. Usa **auto-instrumentación** vía el agent de Microsoft (`applicationinsights-agent.jar` ya viene dentro del Dockerfile), por lo que no requiere tocar código para tener:

- Trazas distribuidas end-to-end (HTTP, JDBC, dependencias salientes).
- Métricas custom adicionales.
- Excepciones con stack trace.
- Live Metrics para debugging en tiempo real.

El Dockerfile está construido en **multi-stage**: la primera etapa compila con Maven+JDK 21, la segunda copia el JAR a una imagen JRE 21 liviana con el agent de App Insights descargado en build. Tamaño final: ~250 MB.

---

## 9. Calidad y testing

### 9.1 Pirámide de tests

```
              ┌────────────────────┐
              │  Contract (Newman) │   Paridad HTTP Node vs Spring
              ├────────────────────┤
              │   Integration IT   │   Testcontainers (MS SQL real)
              │   + WireMock       │   + mock de Google OAuth
              ├────────────────────┤
              │       Web          │   MockMvc + @WebMvcTest
              │   (controllers)    │   32 tests
              ├────────────────────┤
              │   Application      │   Servicios con puertos mockeados
              │   (use cases)      │   37 tests
              ├────────────────────┤
              │     Domain         │   Records, value objects, invariantes
              │   (puro, sin       │   27 tests
              │    Spring)         │   corre en milisegundos
              └────────────────────┘
```

### 9.2 Suite completa (159 tests, 18 clases)

| Suite | Tests | Cubre |
|---|---|---|
| `domain.model.*Test` | 24 | Records, value objects, invariantes (`Correo`, `PasswordHash`, `RolUsuario`, `Usuario`). |
| `domain.exception.DomainExceptionTest` | 3 | Jerarquía de excepciones. |
| `application.UsuarioServiceTest` | 37 | 10 use cases con puertos mockeados: registro, login, OAuth, lockout, refresh, logout, perfil, listado paginado, CRUD. |
| `application.service.*Test` (otros) | 8 | `EmailVerificationService`, `CatalogoPropiedadesService`. |
| `infrastructure.persistence.UsuarioRepositoryAdapterTest` | 13 | `@DataJpaTest` con H2 + Flyway, queries, mapeos. |
| `infrastructure.security.JwtTokenProviderAdapterTest` | 7 | Generación y parseo de access/refresh/email tokens. |
| `infrastructure.BcryptPasswordEncoderAdapterTest` | 6 | Hashing y verificación. |
| `infrastructure.config.AuthMetricsTest` | 8 | Los 9 counters. |
| `infrastructure.web.ApimHeaderFilterTest` | 7 | Sub-key, mTLS, bypass en dev. |
| `web.AuthControllerTest` | 18 | MockMvc: login, registro, google, github, profile, refresh, logout, verify, send-verification. |
| `web.UsuarioControllerTest` | 10 | MockMvc: listar, get, update, delete + 403 cross-user. |
| `web.PropiedadesControllerTest` | 5 | Filtros, paginación, normalización de búsqueda (tildes, mayúsculas). |
| `web.HealthControllerTest` | 1 | Health endpoint con BD. |
| `web.MetricsEndpointTest` | 3 | Health + AuthMetrics + counters. |
| `web.ValidationTest` | 16 | Bean Validation en DTOs. |
| `web.GlobalExceptionHandlerTest` | 2 | Mapeo de excepciones a HTTP. |
| **Total unit + web** | **159** | |

### 9.3 Tests de integración (Testcontainers + WireMock)

| Test | Qué hace |
|---|---|
| `UsuarioPersistenceMsSqlIT` | Levanta un contenedor de **MS SQL Server 2022** real con Testcontainers, corre migraciones Flyway y prueba el adapter JPA contra el motor de producción. |
| `GoogleTokenVerifierWireMockIT` | Levanta un **WireMock** que simula los endpoints de Google y prueba el `GoogleTokenVerifierAdapter` con tokens emitidos por el mock. |

Se ejecutan con el profile `integration` (requiere Docker disponible):

```bash
mvn -B -ntp test -Pintegration
```

### 9.4 Contract tests (Newman)

La suite `contract-tests/paridad.postman_collection.json` define el contrato HTTP canónico. Se ejecuta contra el servicio local o contra el despliegue en APIM con:

```bash
./contract-tests/run.sh
```

Para validar **paridad con el servicio Node original**:

```bash
NODE_URL=http://localhost:3000 SPRING_URL=http://localhost:8080 \
  ./contract-tests/compare-node-vs-spring.sh
```

Esto garantiza que el frontend puede migrar de Node a Spring sin cambiar una sola línea de cliente.

### 9.5 CI con GitHub Actions

Workflow en `.github/workflows/` que:

1. Compila con `mvn -B -ntp clean compile` (Java 21, cache de dependencias).
2. Corre los 159 tests unitarios y web.
3. Empaqueta el JAR sin tests.
4. Reporta a Codecov / SonarCloud si están configurados en el repo.

---

## 10. Despliegue y operaciones

### 10.1 Contenedor

**Multi-stage Dockerfile**:

- **Stage 1** (`maven:3.9.9-eclipse-temurin-21`): cache de dependencias con `dependency:go-offline`, luego `mvn clean package -DskipTests`. Produce `target/ms-usuarios-boot.jar` (~67 MB).
- **Stage 2** (`eclipse-temurin:21-jre-jammy`): descarga el **Application Insights agent** 3.7.8 en build, copia el JAR y la config JSON del agent, crea usuario no-root `app`, expone `8080` y define un `HEALTHCHECK` contra `/actuator/health`.

### 10.2 docker-compose local

Levanta **SQL Server 2022** + la app en una sola red bridge:

```yaml
services:
  sqlserver:    # mcr.microsoft.com/mssql/server:2022-latest
    healthcheck:  # SELECT 1 cada 10s
  app:          # ms-usuarios-boot:local, depende de sqlserver:healthy
    profiles:   # dev con JDBC → sqlserver del compose
    ports:      # 8080:8080
```

### 10.3 Profiles de Spring

| Profile | Propósito |
|---|---|
| `dev` | Conexión a SQL Server local (docker-compose), CORS abierto a `*`, APIM bypass. |
| `test` | H2 en memoria, migraciones Flyway desde `classpath:db/migration`, APIM bypass. |
| `azure-test` | Conexión a Azure SQL de test, CORS solo para el SWA, APIM habilitado. |
| `prod` | Conexión a Azure SQL de producción, todas las seguridades activas. |
| `azure-insights` (perfil adicional) | Activa `spring-cloud-azure-starter-monitor` y carga el agent Java. |

### 10.4 Variables de entorno

Críticas (sin defaults seguros):

| Variable | Obligatoria | Uso |
|---|---|---|
| `JWT_SECRET` | **Sí** | Firma de access y refresh tokens. Mínimo 32 bytes. |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | **Sí** | Conexión a SQL Server. |
| `APIM_SUBSCRIPTION_KEY` | Prod | Validación de subscription key en APIM. |
| `APIM_VALIDATE_CLIENT_CERT` | Prod (opcional) | Activa validación mTLS. |
| `GOOGLE_CLIENT_ID` | Si se usa Google | Audience del `id_token`. |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | Si se usa GitHub | Authorization Code flow. |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | Prod | Conexión a App Insights. |
| `RL_*` | Opcional | Rate limit: `auth`, `read`, `write`, `general`. Defaults: 5/200/50/100 por 15 min. |

### 10.5 Plan de cutover (resumen)

Documentado en detalle en `docs/CUTOVER_RUNBOOK.md`. Cinco fases con criterios de rollback:

1. **Despliegue sombra** en APIM (100% tráfico duplicado, no se sirven respuestas).
2. **Canary 5%** de tráfico real al nuevo servicio.
3. **Canary 25%** con monitoreo intensivo de métricas.
4. **Canary 100%** con el servicio Node en standby.
5. **Deprecación** del servicio Node.

---

## 11. Catálogo de propiedades (mock transitorio)

Mientras el microservicio `ms-propiedades` está en desarrollo, este servicio expone un **catálogo en memoria** con 24 propiedades hardcodeadas (IDs `prop-001` a `prop-024`) que el frontend usa para mostrar la UI. Implementa:

- **Búsqueda full-text** con normalización Unicode (tildes y mayúsculas).
- **Filtros** por provincia, tipo, rango de precio, dueño.
- **Paginación** validada (página ≥ 1, tamaño 1-100).
- **Endpoint público** (no requiere JWT) para que la landing page funcione sin login.

Cuando `ms-propiedades` esté listo, el `CatalogoPropiedadesUseCase` se reemplaza por un adapter que hace `WebClient` contra el servicio real. El dominio no se toca.

---

## 12. Lecciones aprendidas

1. **El dominio sin Spring acelera el feedback loop.** Reescribir un caso de uso y correr sus tests tarda < 1 segundo. En el servicio Node, el ciclo era de varios segundos por la carga de módulos y la inicialización de Express.

2. **Java 21 + records eliminaron una clase entera de bugs.** Los `equals`/`hashCode` mal hechos, los DTOs mutables compartidos, los `null` en lugares incorrectos: todo se reduce a "el record valida en su constructor compacto".

3. **Los tokens revocados en BD derrotan al "JWT es stateless"**. Sí, requiere ir a BD en cada request, pero `tokens_revocados` se indexa por `jti` y se limpia periódicamente (`cleanupAfterDays: 7`). El trade-off es favorable: logout real es un requisito del producto.

4. **APIM + filtro de sub-key da defense in depth sin código custom.** La sub-key se valida en el filtro de Java como una segunda red, incluso si alguien bypasea APIM.

5. **El equipo de frontend agradece los IDs legibles.** `usr-083` es mil veces más fácil de debuggear en logs que `b40efb01-fab0-41ea-88b7-aa93f3b21ad3`. El costo: hay que abstraer la generación con un port.

6. **Los contract tests con Newman salvaron una migración entera.** Comparar la respuesta de Node y Spring para los mismos inputs fue lo que detectó las tres diferencias de schema en los endpoints de auth antes de que llegaran al frontend.

---

## 13. Referencias

| Recurso | Ubicación |
|---|---|
| Código del servicio | `src/main/java/com/arrendamientos/usuarios/` |
| Tests | `src/test/java/com/arrendamientos/usuarios/` |
| Migraciones | `src/main/resources/db/migration/` |
| Dockerfile | `Dockerfile` |
| Compose local | `docker-compose.yml` |
| Contract tests | `contract-tests/` |
| Plan de cutover | `docs/CUTOVER_RUNBOOK.md` |
| Coordinación cross-team | `../COORDINACION_MICROSERVICIOS.md` |
| Infraestructura Azure | `../AZURE_INFRASTRUCTURE.md` |
| Servicio Node original | `../MS-Usuarios-Plataforma-Arrendamientos-CR/` |
| Frontend | `../Plataforma-de-Arrendamientos-CR/` |
