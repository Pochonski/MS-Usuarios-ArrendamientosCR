# MS-Usuarios Boot

Reescritura del microservicio `MS-Usuarios-Plataforma-Arrendamientos-CR` (Node 20 + TypeScript + Express) en **Spring Boot 3.3** sobre **Java 21 LTS**, organizada con **arquitectura hexagonal** (puertos y adaptadores).

> Convive con el código Node original mientras se valida paridad. El servicio Node sigue siendo la fuente de verdad hasta el cutover.

## Stack

| Componente | Tecnología |
|------------|------------|
| Runtime | Java 21 LTS (records, pattern matching) |
| Framework | Spring Boot 3.3.4 |
| Build | Maven 3.9 |
| Persistencia | Spring Data JPA + Hibernate 6 + `mssql-jdbc` |
| Migraciones | Flyway 10 (V1, V2, V3) |
| Validación | Jakarta Bean Validation |
| JWT | `io.jsonwebtoken:jjwt 0.12.6` |
| Google OAuth | `google-api-client 2.8.0` (`GoogleIdTokenVerifier`) |
| Rate limit | `bucket4j 8.14.0` |
| OpenAPI | `springdoc-openapi 2.6.0` |
| Seguridad | Spring Security 6 + BCrypt 10 |
| Observabilidad | Micrometer Prometheus + Azure Application Insights (profile `azure-insights`) |
| Integración | Testcontainers 1.20.3 + WireMock 3.9.1 |
| Contract tests | Newman (Postman CLI) |
| Tests | JUnit 5 + Mockito + AssertJ + H2 (test) + Spring Boot Test |

## Estado del proyecto

| Fase | Descripción | Estado |
|------|-------------|--------|
| F0  | Skeleton: `pom.xml`, `application.yml`, migrations, HealthController | ✅ |
| F1  | Persistencia JPA + adaptadores | ✅ |
| F2  | Dominio + use cases + servicios | ✅ |
| F3  | Web + seguridad (APIM, JWT, BCrypt) | ✅ |
| F4  | Rate limit, validación, OpenAPI | ✅ |
| F5  | Tests: **159 tests** (domain + application + infra + web + JPA + APIM filter) | ✅ |
| F6  | Docker (multi-stage Dockerfile + docker-compose con SQL Server 2022) | ✅ |
| F7  | GitHub Actions CI (Java 21 + cache Maven) | ✅ |
| O1  | Testcontainers (MS SQL 2022 + perfil `integration`) | ✅ |
| O2  | Testcontainers integration test contra MS SQL real | ✅ |
| O3  | WireMock (mock de Google OAuth endpoints) | ✅ |
| O4  | Micrometer Prometheus + 9 counters de negocio + Azure Monitor (profile) | ✅ |
| O5  | Newman contract tests (paridad HTTP) + script de comparación Node vs Spring | ✅ |
| O6  | APIM cutover runbook (5 fases con rollback) | ✅ |
| F8  | Despliegue sombra en APIM | ⏳ |
| F9  | Cutover y deprecación del servicio Node | ⏳ |

## Estructura (hexagonal)

```
src/main/java/com/arrendamientos/usuarios/
├── domain/                                # NÚCLEO — sin Spring/JPA
│   ├── model/        Usuario, UsuarioId, Correo, PasswordHash, RolUsuario, GoogleUserInfo, UsuarioView
│   ├── exception/    DomainException + 8 excepciones específicas
│   └── port/
│       ├── in/       12 use cases
│       └── out/      7 ports
├── application/
│   ├── service/      UsuarioService (10 use cases) + EmailVerificationService (2)
│   └── dto/          Commands y AuthResult
└── infrastructure/                       # ADAPTADORES
    ├── config/       AppProperties, SecurityConfig, OpenApiConfig, AuthMetrics
    ├── persistence/  JPA entities + repos + mappers + adapters
    ├── security/     JwtAuthenticationFilter, JwtTokenProviderAdapter, BcryptPasswordEncoderAdapter
    ├── web/          AuthController, UsuarioController, HealthController, GlobalExceptionHandler, ApimHeaderFilter
    ├── ratelimit/    RateLimitFilter (Bucket4j)
    ├── google/       GoogleTokenVerifierAdapter
    └── email/        LoggingEmailSenderAdapter

src/test/java/com/arrendamientos/usuarios/
├── domain/                                # Tests puros (sin Spring)
├── application/                           # UsuarioServiceTest con puertos mockeados
├── infrastructure/                       # JPA @DataJpaTest, JWT, BCrypt, APIM filter
├── integration/                           # Testcontainers + WireMock
├── web/                                   # MockMvc controllers
└── testsupport/                           # TestJwt, MsSqlTestContainer, GoogleOAuthWireMock

contract-tests/
├── paridad.postman_collection.json       # Newman contract suite
├── local.postman_environment.json
├── azure-sombra.postman_environment.json
├── run.sh                                # Newman runner
└── compare-node-vs-spring.sh             # Diff Node vs Spring

docs/
└── CUTOVER_RUNBOOK.md                     # 5 fases de cutover con rollback
```

## Endpoints (compatibles con la versión Node)

| Método | Ruta | Auth |
|--------|------|------|
| POST | `/api/auth/login` | público |
| POST | `/api/auth/registro` | público |
| POST | `/api/auth/google` | público |
| GET | `/api/auth/profile` | JWT |
| POST | `/api/auth/refresh` | JWT |
| POST | `/api/auth/logout` | JWT |
| GET | `/api/auth/verify-email/{token}` | público |
| POST | `/api/auth/send-verification-email` | JWT |
| GET | `/api/usuarios` | JWT |
| GET | `/api/usuario/{id}` | JWT |
| PUT | `/api/usuario/{id}` | JWT (solo el propio) |
| DELETE | `/api/usuario/{id}` | JWT (solo el propio) |
| GET | `/api/health` | público |
| GET | `/actuator/health` | público |
| GET | `/v3/api-docs` | público |
| GET | `/swagger-ui.html` | público |

## Comandos

```bash
# Compilar
mvn -B -ntp clean compile

# Tests (159 tests, perfil `test` con H2)
mvn -B -ntp test

# Integration tests con MS SQL real (requiere Docker)
mvn -B -ntp test -Pintegration

# Empaquetar (fat jar 67 MB)
mvn -B -ntp clean package -DskipTests

# Ejecutar en dev (perfil test con H2)
mvn -B -ntp spring-boot:run -Dspring-boot.run.profiles=test

# Ejecutar con SQL Server real (docker)
docker compose up -d
mvn -B -ntp spring-boot:run -Dspring-boot.run.profiles=dev

# Levantar con Docker (todo)
docker compose up --build

# Build de imagen Docker local
docker build -t ms-usuarios-boot:local .

# Contract tests contra el servicio local
./contract-tests/run.sh

# Comparar Node vs Spring
NODE_URL=http://localhost:3000 SPRING_URL=http://localhost:8080 \
  ./contract-tests/compare-node-vs-spring.sh

# Habilitar Application Insights (prod)
mvn -B -ntp clean package -Pazure-insights
APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=... \
  java -jar target/ms-usuarios-boot.jar
```

## Variables de entorno

Copiar `.env.example` → `.env`. Las críticas:

```bash
DB_HOST=your-server.database.windows.net
DB_NAME=usuarios_db
DB_USER=...
DB_PASSWORD=...
JWT_SECRET=...                 # obligatorio (mín 32 bytes)
GOOGLE_CLIENT_ID=...           # para /api/auth/google
APIM_SUBSCRIPTION_KEY=...      # en prod
APIM_VALIDATE_CLIENT_CERT=false
APPLICATIONINSIGHTS_CONNECTION_STRING=...  # opcional
```

## Tests

**159 tests, 18 clases, todas las capas cubiertas:**

```bash
mvn -B -ntp test
```

| Suite | Tests | Cobertura |
|-------|-------|-----------|
| `domain.model.*Test` | 24 | Records, value objects, invariantes |
| `domain.exception.DomainExceptionTest` | 3 | Jerarquía de excepciones |
| `application.UsuarioServiceTest` | 37 | 10 use cases con puertos mockeados |
| `infrastructure.persistence.UsuarioRepositoryAdapterTest` | 13 | `@DataJpaTest` con H2 + Flyway |
| `infrastructure.security.JwtTokenProviderAdapterTest` | 7 | Generación/parseo JWT |
| `infrastructure.BcryptPasswordEncoderAdapterTest` | 6 | Hashing |
| `infrastructure.config.AuthMetricsTest` | 8 | 9 counters de negocio |
| `infrastructure.web.ApimHeaderFilterTest` | 7 | Sub-key, mTLS, dev bypass |
| `web.AuthControllerTest` | 18 | MockMvc: login/registro/google/profile/refresh/logout/verify |
| `web.UsuarioControllerTest` | 10 | MockMvc: list/get/update/delete + 403 cross-user |
| `web.HealthControllerTest` | 1 | Health endpoint con DB |
| `web.MetricsEndpointTest` | 3 | Health + AuthMetrics + counters |
| `web.ValidationTest` | 16 | Bean Validation DTOs |

## Observabilidad

9 counters de negocio expuestos vía Micrometer Prometheus:

```
auth_login_success_total
auth_login_failure_total
auth_register_success_total
auth_register_conflict_total
auth_google_success_total
auth_google_failure_total
auth_account_locked_total
auth_token_refresh_total
auth_logout_total
```

Métricas HTTP estándar (latencia P50/P95/P99) + JVM (heap, GC, threads) + DB (HikariCP).

Para Application Insights en producción:
```bash
mvn -B -ntp clean package -Pazure-insights
APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=... \
  java -jar target/ms-usuarios-boot.jar
```

## Referencias

- [docs/CUTOVER_RUNBOOK.md](docs/CUTOVER_RUNBOOK.md) — Plan de despliegue 5 fases con rollback
- [contract-tests/](contract-tests/) — Newman contract suite + scripts de comparación
- [`../README.md`](../README.md) — README del repo raíz (servicio Node original)
- [`../PROJECT_STRUCTURE.md`](../PROJECT_STRUCTURE.md) — Estructura del servicio Node
