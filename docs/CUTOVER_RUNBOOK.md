# APIM Cutover Runbook — MS-Usuarios Node → Spring Boot 3

> Procedimiento paso a paso para migrar el tráfico del servicio Node actual al nuevo servicio Spring Boot 3 en Azure API Management, minimizando downtime y con capacidad de rollback.

## Estado actual

| Servicio | Tecnología | Estado | Endpoint APIM |
|----------|-------------|--------|---------------|
| `MS-Usuarios` (viejo) | Node 20 + TypeScript + Express | **PRODUCCIÓN** | `https://plataforma-arrendamientos-api.azure-api.net/auth/...` |
| `MS-Usuarios-Boot` (nuevo) | Spring Boot 3 + Java 21 | **Pendiente despliegue** | (a configurar) |

## Pre-requisitos

- [x] 159/159 tests pasan localmente (`mvn clean verify`)
- [x] `mvn package` produce JAR ejecutable
- [x] Docker image build + smoke test local
- [x] Migración `V1`/`V2`/`V3` de Flyway probada contra Azure SQL (sin cambios de schema)
- [x] `application-prod.yml` configurado
- [ ] App Service plan + plan de scaling para Java 21
- [ ] Application Insights configurado (opcional, vía profile `azure-insights`)

## Fases

### Fase 1 — Deploy sombra (0% tráfico)

1. **Build de la imagen Docker**:
   ```bash
   cd ms-usuarios-boot
   docker build -t ms-usuarios-boot:v1.0.0 .
   ```

2. **Push a Azure Container Registry (ACR)**:
   ```bash
   az acr login --name <acr-name>
   docker tag ms-usuarios-boot:v1.0.0 <acr-name>.azurecr.io/ms-usuarios-boot:v1.0.0
   docker push <acr-name>.azurecr.io/ms-usuarios-boot:v1.0.0
   ```

3. **Crear App Service (Java 21)**:
   ```bash
   az appservice plan create --name plan-ms-usuarios --resource-group <rg> --sku B2 --is-linux
   az webapp create --name ms-usuarios-boot --resource-group <rg> \
     --plan plan-ms-usuarios \
     --deployment-container-image-name <acr-name>.azurecr.io/ms-usuarios-boot:v1.0.0 \
     --runtime "JAVA:21-java21"
   ```

4. **Configurar variables de entorno** (en Azure Portal → App Service → Configuration):
   ```
   SPRING_PROFILES_ACTIVE=prod
   DB_HOST=<azure-sql-server>.database.windows.net
   DB_NAME=usuarios_db
   DB_USER=<user>
   DB_PASSWORD=<password-from-keyvault>
   JWT_SECRET=<jwt-secret-from-keyvault>
   GOOGLE_CLIENT_ID=<google-client-id>
   APIM_SUBSCRIPTION_KEY=<apim-sub-key>
   APIM_VALIDATE_CLIENT_CERT=false
   WEBSITES_PORT=8080
   ```

5. **Probar health endpoint directo** (sin APIM):
   ```bash
   curl https://ms-usuarios-boot.azurewebsites.net/actuator/health
   ```

6. **Validar paridad con contract tests**:
   ```bash
   NODE_URL=https://plataforma-arrendamientos-api.azure-api.net/auth \
   SPRING_URL=https://ms-usuarios-boot.azurewebsites.net \
   ./contract-tests/compare-node-vs-spring.sh
   ```

### Fase 2 — Smoke en APIM (5% tráfico)

1. **Agregar nueva API a APIM** (sin reemplazar la existente):
   - Portal Azure → API Management → plataforma-arrendamientos-api → APIs → Add API
   - Name: `MS-Usuarios-Boot`
   - Web service URL: `https://ms-usuarios-boot.azurewebsites.net`
   - Path: `ms-usuarios-boot`
   - APIs URL suffix: `ms-usuarios-boot`

2. **Aplicar la misma política CORS** que la API original (ver `APIM_CONFIG.md`).

3. **Configurar routing al 5%**:
   - En la API original `MS-Usuarios`, agregar un `choose` policy al `backend`:
   ```xml
   <backend>
     <base />
     <choose>
       <when condition="@(context.Request.Headers.GetValueByKey("X-Test-Route") == "spring")">
         <set-backend-service base-url="https://ms-usuarios-boot.azurewebsites.net" />
       </when>
     </choose>
   </backend>
   ```

4. **Enviar header de test** desde Postman/curl:
   ```bash
   curl -H "X-Test-Route: spring" https://plataforma-arrendamientos-api.azure-api.net/auth/login ...
   ```

5. **Verificar Application Insights / logs**:
   ```bash
   az webapp log tail --name ms-usuarios-boot --resource-group <rg>
   ```

### Fase 3 — Canary 25% / 50% / 100%

1. **Modificar la policy** para hacer routing probabilístico:
   ```xml
   <backend>
     <base />
     <choose>
       <when condition="@(new Random().Next(100) < 25)">
         <set-backend-service base-url="https://ms-usuarios-boot.azurewebsites.net" />
       </when>
     </choose>
   </backend>
   ```

2. **Monitorear métricas** durante 24-48h en cada escalón:
   - Latencia P50/P95/P99
   - Tasa de errores 4xx/5xx
   - Counters de `auth.login.success/failure`, `auth.account.locked`
   - Logs de APIM con `BackendId` header

3. **Comparar respuestas JSON** con el script `compare-node-vs-spring.sh` en producción:
   ```bash
   # Sample 100 requests aleatorios
   for i in $(seq 1 100); do
     ./contract-tests/compare-node-vs-spring.sh 2>&1 | tee -a /tmp/paridad-$(date +%Y%m%d).log
   done
   ```

4. **Si todo OK**, escalar a 50% → 100% en pasos de 24h.

### Fase 4 — Cutover completo (100% Spring Boot)

1. **Reemplazar la policy de la API original**:
   ```xml
   <backend>
     <set-backend-service base-url="https://ms-usuarios-boot.azurewebsites.net" />
   </backend>
   ```

2. **Verificar 0 errores** durante 1h.

3. **Mantener el App Service Node en standby** durante 1 semana.

### Fase 5 — Deprecación del servicio Node

1. **Apuntar App Service Node a un endpoint "deprecated"** que retorne 410 Gone:
   ```javascript
   app.use((req, res) => {
     res.status(410).json({
       error: 'Gone',
       message: 'This service has been migrated to Spring Boot. Please update your client.'
     });
   });
   ```

2. **Esperar 7 días** sin tráfico.

3. **Apagar y eliminar** el App Service Node + plan correspondiente.

## Rollback

Si en cualquier fase se detectan errores críticos:

1. **Revertir la policy de APIM** al `base-url` del servicio Node:
   ```xml
   <backend>
     <set-backend-service base-url="https://ms-usuarios-node.azurewebsites.net" />
   </backend>
   ```

2. **Verificar** que la API original sigue respondiendo.

3. **Investigar** logs de Spring Boot (`az webapp log tail`).

4. **Fix + re-deploy** (incrementar versión de imagen: `v1.0.1`, `v1.0.2`...).

## Checklist de cutover

- [ ] Build verde
- [ ] Tests verdes
- [ ] Docker image pusheado a ACR
- [ ] App Service Java 21 desplegado
- [ ] Variables de entorno configuradas
- [ ] Health endpoint responde
- [ ] Contract tests pasan contra Spring Boot
- [ ] Paridad JSON con Node validada
- [ ] Application Insights recibe métricas
- [ ] Canary 5% OK durante 24h
- [ ] Canary 25% OK durante 24h
- [ ] Canary 50% OK durante 24h
- [ ] Canary 100% OK durante 48h
- [ ] Node deprecado (410 Gone)
- [ ] Node eliminado tras 7 días
- [ ] APIM policy simplificada (apunta sólo a Spring)

## Contactos de emergencia

- **APIM issues**: revisar Azure Portal → API Management → plataforma-arrendamientos-api
- **App Service down**: `az webapp restart --name ms-usuarios-boot --resource-group <rg>`
- **SQL connection issues**: revisar firewall rules de Azure SQL + connection string
- **JWT issues**: verificar `JWT_SECRET` en Key Vault, rotar si es necesario
