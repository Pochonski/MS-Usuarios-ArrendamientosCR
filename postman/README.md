# Postman Collection - MS-Usuarios Boot

Colección completa de Postman para el microservicio `MS-Usuarios-Boot` (Spring Boot 3.3 + Java 21 + arquitectura hexagonal). Cubre los 16 endpoints públicos de la API (9 de auth incluyendo GitHub + 3 de usuarios + health + 2 OpenAPI).

## 📦 Archivos

| Archivo | Propósito |
|---------|-----------|
| `MS-Usuarios-Boot.postman_collection.json` | Colección principal (Postman v2.1) |
| `azure-swa.postman_environment.json` | **Recomendado.** Apunta al SWA → proxied al App Service. Funciona siempre, no requiere deslinkear. |
| `azure.postman_environment.json` | Apunta directo al App Service. **Solo funciona si deslinkeas el backend** (ver `../AZURE_INFRASTRUCTURE.md`). Útil para debug. |
| `local.postman_environment.json` | Environment apuntando a `localhost:8080` (docker compose / mvn) |
| `node.postman_environment.json` | Environment apuntando al servicio Node original (paridad histórica, ya no se usa) |

## 🚀 Cómo importar en Postman

### Opción 1 — Drag & drop
1. Abrir Postman desktop o web (https://web.postman.com)
2. Arrastrar los 4 archivos `.json` a la ventana principal
3. Postman los detecta automáticamente como Collection + Environments

### Opción 2 — File → Import
1. Click en `Import` (esquina superior izquierda)
2. Seleccionar `Upload Files`
3. Elegir los 4 archivos `.json`
4. Click `Import`

### Opción 3 — Importar desde URL (no aplica aquí, son locales)

## ⚙️ Configuración inicial

Después de importar, seleccioná el environment que vas a usar:

| Environment | Cuándo usarlo |
|-------------|---------------|
| **MS-Usuarios Boot - Azure (producción)** | Probar el deploy real en Azure (https://arrendamientos-ms-users-boot.azurewebsites.net) |
| **MS-Usuarios Boot - Local (docker)** | Probar la app corriendo localmente (docker compose up o mvn spring-boot:run) |
| **MS-Usuarios Boot - Node (paridad)** | Comparar contra el servicio Node original (Express + TypeScript) |

Para seleccionar: dropdown arriba a la derecha en Postman → "No environment" → elegir el que corresponda.

## 🧭 Flujo recomendado de uso

La colección está organizada en 6 secciones numeradas. **Ejecutá los requests en este orden** para que el flujo funcione (los scripts `prerequest` y `test` capturan el JWT automáticamente):

### 0️⃣ `00. Info` - Punto de entrada
- `00. README` - Descripción general

### 1️⃣ `01. Health Check` - Verificar conectividad
- `01.1 Health (custom endpoint)` - `/api/health` con verificación de Azure SQL
- `01.2 Spring Boot Actuator Health` - `/actuator/health` (Spring Boot nativo)

### 2️⃣ `02. Auth - Registro y Login` - Crear usuario
- `02.1 Registro de nuevo usuario` ⭐ - **Ejecutar primero**. Genera un correo único con timestamp, registra el usuario, captura el `auth_token`, `refresh_token` y `user_id` automáticamente
- `02.2 Registro con correo duplicado` - Verifica 409 Conflict
- `02.3 Registro con validación fallida` - Verifica 400 Bad Request
- `02.4 Login con credenciales correctas` - Genera nuevo token
- `02.5 Login con password incorrecto` - Verifica 401 + intentos fallidos
- `02.6 Login con email normalizado` - Verifica normalización a minúsculas

### 3️⃣ `03. Auth - Profile, Refresh, Google (token activo)` - Verificar JWT y OAuth Google
- `03.1 GET /api/auth/profile (con JWT)` - Usa el token capturado en 02.1
- `03.2 GET /api/auth/profile SIN token` - Verifica 401
- `03.3 POST /api/auth/refresh` - Rota el refresh token
- `03.4 POST /api/auth/google con body vacío` - Verifica 400 (validación `@NotBlank` en `googleToken`)
- `03.5 POST /api/auth/google con token inválido` - Verifica 400 (Google verifier throws `IllegalArgumentException`)

### 4️⃣ `04. Auth - Email Verification` - Verificación de correo
- `04.1 POST /api/auth/send-verification-email` - Simula envío (loguea el link)
- `04.2 GET /api/auth/verify-email/{token}` - Verifica token inválido

### 5️⃣ `05. Usuarios - CRUD` - Operaciones sobre usuarios
- `05.1 GET /api/usuarios` - Listar paginado
- `05.2 GET /api/usuarios?email=...` - Buscar por prefijo
- `05.3 GET /api/usuarios?rol=dueno` - Filtrar por rol
- `05.4 GET /api/usuario/{id}` - Obtener por ID
- `05.5 GET /api/usuario/{id} no existente` - Verifica 404
- `05.6 PUT /api/usuario/{propio}` - Actualizar perfil propio
- `05.7 PUT /api/usuario/{otro}` - Verifica 403 (no podés editar a otros)
- `05.8 PUT con validación fallida` - Verifica 400
- `05.9 DELETE /api/usuario/{otro}` - Verifica 403
- `05.10 DELETE /api/usuario/{propio}` - ⚠️ **Elimina el usuario creado en 02.1**

### 6️⃣ `06. Auth - GitHub OAuth` - Login con GitHub (Authorization Code flow)
- `06.1 POST /api/auth/github con body válido` - ⚠️ **Request de documentación, no ejecutable en CI.** El `code` real solo se obtiene via el redirect flow de GitHub OAuth App (`https://github.com/login/oauth/authorize?client_id=...&redirect_uri=...&scope=read:user+user:email` → callback en el SPA → backend canjea el code por access_token). El test script acepta 200 (con `code` real) o 400 (con `code` dummy/expirado) y valida la shape del body en ambos casos.
- `06.2 POST /api/auth/github con body vacío` - Verifica 400 (`@NotBlank` en `code` y `redirectUri`)
- `06.3 POST /api/auth/github con code inválido` - Verifica 400 (el adapter tira `IllegalArgumentException` cuando GitHub rechaza el code)

### 7️⃣ `07. OpenAPI / Swagger` - Documentación auto-generada
- `07.1 OpenAPI 3 JSON spec` - `/api/v3/api-docs` (bajo `/api/` para que el SWA proxy lo sirva)
- `07.2 Swagger UI` - `/api/swagger-ui.html`

### 8️⃣ `08. Cleanup - Logout` (⚠️ ejecutar al final del flujo)
- `08.1 POST /api/auth/logout` - ⚠️ **Revoca el `{{auth_token}}` principal**. Es el ÚLTIMO request del flujo. Después de ejecutarlo, el token queda en la tabla de revocados — si querés re-ejecutar la collection, tenés que volver a 02.1 (registro) o 02.4 (login) para obtener uno nuevo.
- `08.2 POST /api/auth/logout SIN token` - Verifica 401 (independiente de 08.1, no necesita token)

## 🧪 Ejecutar toda la colección (CLI)

Con **Newman** (CLI de Postman):

```bash
# Instalar newman globalmente
npm install -g newman

# Ejecutar contra Azure
newman run postman/MS-Usuarios-Boot.postman_collection.json \
  --environment postman/azure.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export report.html

# Ejecutar contra local
newman run postman/MS-Usuarios-Boot.postman_collection.json \
  --environment postman/local.postman_environment.json

# Ejecutar contra Azure (vía SWA proxy — RECOMENDADO)
newman run postman/MS-Usuarios-Boot.postman_collection.json \
  --environment postman/azure-swa.postman_environment.json \
  --env-var "test_email=$EMAIL"

# Ejecutar contra Azure (directo al App Service — solo si deslinkeás el backend)
newman run postman/MS-Usuarios-Boot.postman_collection.json \
  --environment postman/azure.postman_environment.json

# Ejecutar contra Node (paridad histórica)
newman run postman/MS-Usuarios-Boot.postman_collection.json \
  --environment postman/node.postman_environment.json
```

> ⚠️ **Desde 2026-06-08 el App Service está linkeado como backend del SWA**. El identity provider `Azure Static Web Apps (Linked)` rechaza tráfico directo al App Service. Para Newman contra prod, **usar siempre `azure-swa.postman_environment.json`**. El `azure.postman_environment.json` solo sirve para debugging local/después de unlink.

> ℹ️ **Variable `test_email`**: los 4 environments (`azure-swa`, `azure`, `local`, `node`) tienen un valor default `postman+manual@postman-test.com`. Si corrés la collection en **Postman UI** sin cambiarlo, vas a usar ese email — si ya existe en la BD, el 02.2 va a fallar con 409 (esperado) y 02.4 va a tirar 401 hasta que cambies `test_email` a algo único. Los tests 02.1/02.2/02.4/02.5 tienen un guard que falla con mensaje claro si `test_email` está vacío.

> ℹ️ El `base_url` por default en la collection apunta al SWA (`https://agreeable-ground-0b1436910.6.azurestaticapps.net`), así que si abrís la collection sin seleccionar un env, igual funciona vía el proxy `/api/*` del SWA. El item 00. Info (README) tiene un guard que falla con mensaje claro si el env activo no tiene `base_url` configurado.

## 🔐 Sobre los secretos

Esta colección **NO** contiene credenciales en el environment. Los secretos (DB password, JWT secret, Google client ID) se leen desde **Azure Key Vault** en el deploy de producción, o de `.env` en local. La colección sólo maneja la URL base + variables de runtime (token, user_id).

## 🐛 Troubleshooting

| Error | Causa probable | Solución |
|-------|----------------|----------|
| 401 en todo | Token expirado o no capturado | Re-ejecutar `02.1` (captura token nuevo) o `02.4` |
| `getaddrinfo ENOTFOUND {{baseurl}}` (Postman UI) | El env activo no tiene `base_url` seteado o no se activó el env | Verificar que el dropdown de envs en Postman muestra "MS-Usuarios Boot - Azure (vía SWA proxy)". Si dice "No environment", click y elegir el env. Si el env no aparece en la lista, importar de nuevo `postman/azure-swa.postman_environment.json`. El README item (00. Info) tiene un guard que falla con instrucciones claras si detecta este problema. |
| 404 en /api/usuario/{id} | `user_id` variable vacía | Re-ejecutar `02.1` primero |
| 403 en 05.6/05.7 | Estás editando otro usuario | Solo podés editar `{{user_id}}` (tu propio perfil) |
| 429 en login | Demasiados intentos fallidos | Esperar 15 min o usar otro correo |
| Connection refused (local) | App no está corriendo | `mvn spring-boot:run` o `docker compose up` |
| 502/503 (Azure) | App en startup o Key Vault caído | Esperar 1-2 min para arranque, verificar KV en Azure Portal |
| 400 en `06.1` (GitHub) | Estás usando un `{{github_code}}` placeholder (esperado) | Reemplazar por un `code` real. Para obtenerlo: en el browser, abrir `https://github.com/login/oauth/authorize?client_id=<GITHUB_CLIENT_ID>&redirect_uri=http://localhost:5173/auth/github/callback&scope=read:user+user:email`, autorizar la app, copiar el `code` del redirect URI. |
| 400 en `06.1` con `code` real + mensaje "email público" | El perfil de GitHub del usuario tiene el email en privado | El usuario debe hacerlo público en https://github.com/settings/emails (o el adapter podría usar el endpoint `/user/emails` si está configurado — ver `AppProperties.github().useEmailsEndpoint()`). |

## ✅ Estado de validación (Newman CLI vs Azure)

Última corrida con `newman run` contra el SWA (`https://agreeable-ground-0b1436910.6.azurestaticapps.net` vía `azure-swa.postman_environment.json`):

```
requests:       31/31 OK
test-scripts:   29/29 OK
prerequest:      1/1  OK
assertions:     75/75 OK
total run:      8.1s
avg response:   252ms
```

Para reproducir:

```bash
EMAIL="postman+$(date +%s)@postman-test.com"
newman run postman/MS-Usuarios-Boot.postman_collection.json \
  --environment postman/azure-swa.postman_environment.json \
  --env-var "test_email=$EMAIL"
```

## 📚 Recursos adicionales

- **OpenAPI**: https://arrendamientos-ms-users-boot.azurewebsites.net/swagger-ui.html
- **Health**: https://arrendamientos-ms-users-boot.azurewebsites.net/actuator/health
- **Runbook cutover**: `../docs/CUTOVER_RUNBOOK.md`
- **README principal**: `../README.md`
