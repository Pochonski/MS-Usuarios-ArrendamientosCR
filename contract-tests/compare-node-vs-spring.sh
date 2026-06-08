#!/usr/bin/env bash
# Compara las respuestas del servicio Node y el servicio Spring Boot
# ejecutando los mismos requests contra ambos y comparando los JSON.
#
# Uso:
#   NODE_URL=http://localhost:3000 SPRING_URL=http://localhost:8080 ./compare.sh
set -euo pipefail

NODE_URL="${NODE_URL:?NODE_URL requerido (ej. http://localhost:3000)}"
SPRING_URL="${SPRING_URL:?SPRING_URL requerido (ej. http://localhost:8080)}"

ENDPOINTS=(
  "GET|/api/health"
  "GET|/api/usuarios?page=1&limit=5"
  "GET|/api/usuario/usr-001"
)

mkdir -p /tmp/paridad-diff
FAILED=0

for ep in "${ENDPOINTS[@]}"; do
  METHOD="${ep%%|*}"
  PATH_="${ep##*|}"
  LABEL="${METHOD}_$(echo "$PATH_" | tr '/?&=' '____')"

  echo "=== $METHOD $PATH_ ==="

  # Capturar respuestas
  NODE_RESP=$(mktemp)
  SPRING_RESP=$(mktemp)

  HTTP_NODE=$(curl -sS -o "$NODE_RESP" -w "%{http_code}" -X "$METHOD" "$NODE_URL$PATH_" 2>/dev/null || echo "000")
  HTTP_SPRING=$(curl -sS -o "$SPRING_RESP" -w "%{http_code}" -X "$METHOD" "$SPRING_URL$PATH_" 2>/dev/null || echo "000")

  if [ "$HTTP_NODE" != "$HTTP_SPRING" ]; then
    echo "  ✗ Status code distinto: Node=$HTTP_NODE Spring=$HTTP_SPRING"
    FAILED=$((FAILED+1))
  else
    echo "  ✓ Status code: $HTTP_NODE"
  fi

  # Comparar JSON normalizado (jq -S ordena claves)
  if ! diff <(jq -S . "$NODE_RESP" 2>/dev/null) <(jq -S . "$SPRING_RESP" 2>/dev/null) > /tmp/paridad-diff/$LABEL.diff; then
    echo "  ✗ JSON distinto (ver /tmp/paridad-diff/$LABEL.diff)"
    FAILED=$((FAILED+1))
  else
    echo "  ✓ JSON idéntico"
  fi

  rm -f "$NODE_RESP" "$SPRING_RESP"
done

echo
if [ $FAILED -eq 0 ]; then
  echo "✅ Paridad 100%"
  exit 0
else
  echo "❌ $FAILED diferencias detectadas"
  exit 1
fi
