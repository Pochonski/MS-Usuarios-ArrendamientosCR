# Seed de usuarios (2026-06-08)

Archivo: [`seed_usuarios_2026_06_08.sql`](./seed_usuarios_2026_06_08.sql)

## Usuarios insertados (5)

| Id | Nombre | Correo | Rol | Contraseña |
|---|---|---|---|---|
| `usr-083` | María Rodríguez Vargas | `maria.rodriguez@arrendamientoscr.com` | `dueno` | `Arrendamientos2026!01` |
| `usr-084` | Carlos Méndez Solís | `carlos.mendez@arrendamientoscr.com` | `dueno` | `Arrendamientos2026!02` |
| `usr-085` | Ana Lucía Pérez Brenes | `ana.perez@arrendamientoscr.com` | `dueno` | `Arrendamientos2026!03` |
| `usr-086` | Jorge Andrés Vargas Ulate | `jorge.vargas@arrendamientoscr.com` | `inquilino` | `Arrendamientos2026!04` |
| `usr-087` | Daniela Soto Camacho | `daniela.soto@arrendamientoscr.com` | `inquilino` | `Arrendamientos2026!05` |

## ADVERTENCIA

Estos son usuarios de **DEMO** con contraseñas predecibles (patrón
`Arrendamientos2026!0N`). **Rotar antes de cualquier uso productivo real.**

## Características técnicas

- Hashes: **bcrypt cost 10**, prefijo `$2a$` (compatible con `BCryptPasswordEncoder`
  de Spring Security 6 / Java 21).
- Verificación round-trip realizada con `bcryptjs` antes y después de la
  inserción: los 5 hashes validan correctamente.
- Idempotente: cada `INSERT` está protegido por
  `IF NOT EXISTS (SELECT 1 FROM Usuarios WHERE Correo = ...)`. Re-ejecutable
  sin duplicar correos.
- Usa `UPDLOCK, HOLDLOCK, ROWLOCK` sobre la fila `Sequences.UsuarioId`
  para evitar carrera con la app al asignar el siguiente Id.
- Los Ids reservados (en este seed) son `usr-083`…`usr-087` porque
  `Sequences.CurrentValue` estaba en `82` al momento de ejecución.

## Verificación posterior

1. **SQL** — contar usuarios y revisar `Sequences`:
   ```sql
   SELECT Id, Nombre, Correo, Rol, LEFT(ContrasenaHash, 7) AS HashPrefix, FechaRegistro
     FROM Usuarios
     WHERE Correo LIKE '%@arrendamientoscr.com'
     ORDER BY Id;
   SELECT * FROM Sequences WHERE Name = N'UsuarioId';
   ```
2. **Login contra el SWA**:
   ```bash
   SWA="https://agreeable-ground-0b1436910.6.azurestaticapps.net"
   curl -s -X POST "$SWA/api/auth/login" \
     -H "Content-Type: application/json" \
     -d '{"correo":"maria.rodriguez@arrendamientoscr.com","contrasena":"Arrendamientos2026!01"}'
   ```
   Esperado: `200` con `token`, `refreshToken` y objeto `usuario` poblado.

## Cómo aplicar (referencia)

El script está pensado para ejecutarse contra **Azure SQL**
`arrendamientoscr.database.windows.net` / DB `usuarios_db`. Pasos:

1. Abrir firewall temporal para tu IP:
   ```bash
   MY_IP=$(curl -s -4 https://api.ipify.org)
   az sql server firewall-rule create \
     --server arrendamientoscr --resource-group JosephResourceGroup \
     --name "pocho-seed-$(date +%Y-%m-%d)" \
     --start-ip-address "$MY_IP" --end-ip-address "$MY_IP"
   ```
2. Ejecutar con cualquier cliente que soporte batches T-SQL
   (`sqlcmd`, `mssql` Node, `az sql db execute`, etc.).
3. Cerrar firewall:
   ```bash
   az sql server firewall-rule delete \
     --server arrendamientoscr --resource-group JosephResourceGroup \
     --name "pocho-seed-$(date +%Y-%m-%d)"
   ```
