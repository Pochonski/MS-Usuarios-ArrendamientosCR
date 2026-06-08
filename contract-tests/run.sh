#!/usr/bin/env bash
# Ejecuta los contract tests contra el servicio Spring Boot.
# Requiere que el servicio esté corriendo en $BASE_URL.
#
# Uso:
#   ./contract-tests/run.sh                              # usa http://localhost:8080
#   BASE_URL=https://api.ejemplo.com ./contract-tests/run.sh
#   ./contract-tests/run.sh --env contract-tests/azure-sombra.postman_environment.json
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
COLLECTION="$HERE/paridad.postman_collection.json"
ENV_FILE="$HERE/local.postman_environment.json"

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_FILE="$2"; shift 2 ;;
    --collection) COLLECTION="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

# Verificar que Newman está instalado
if ! command -v newman >/dev/null 2>&1; then
  echo "Newman no está instalado. Instalar con: npm install -g newman"
  exit 1
fi

# Verificar conectividad
BASE_URL="${BASE_URL:-$(jq -r '.values[] | select(.key=="base_url") | .value' "$ENV_FILE")}"
echo "Verificando que el servicio responde en $BASE_URL/api/health..."
if ! curl -fsS "$BASE_URL/api/health" >/dev/null 2>&1; then
  echo "ERROR: el servicio no responde en $BASE_URL/api/health"
  echo "Levantar primero con: docker compose up -d"
  exit 1
fi
echo "✓ Servicio OK"
echo

# Ejecutar Newman
newman run "$COLLECTION" \
  --environment "$ENV_FILE" \
  --reporters cli,json \
  --reporter-json-export "$HERE/newman-report.json" \
  --color on

EXIT=$?
echo
echo "Reporte JSON guardado en: $HERE/newman-report.json"
exit $EXIT
