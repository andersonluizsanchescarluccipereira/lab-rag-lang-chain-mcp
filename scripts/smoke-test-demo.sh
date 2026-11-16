#!/usr/bin/env bash
set -euo pipefail
API="${PIX_API_URL:-http://localhost:8083}"
command -v curl >/dev/null || { echo 'Instale curl.' >&2; exit 1; }
command -v python3 >/dev/null || { echo 'Instale python3.' >&2; exit 1; }

status=$(curl -sS -o /dev/null -w '%{http_code}' "$API/api/v1/pix/keys/me")
if [ "$status" != "401" ]; then
  echo "ERRO: consulta sem JWT deveria retornar 401, retornou $status" >&2
  exit 1
fi

auth=$(curl -fsS -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"demo@arqvora.local","password":"Demo@123456"}')
TOKEN=$(printf '%s' "$auth" | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')

keys=$(curl -fsS -H "Authorization: Bearer $TOKEN" "$API/api/v1/pix/keys/me")
printf '%s' "$keys" | python3 -c 'import json,sys; v=json.load(sys.stdin); assert v["totalElements"] >= 2, v; print("OK: minhas chaves")'

lookup=$(curl -fsS -X POST "$API/api/v1/pix/keys/lookup" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"type":"EMAIL","key":"loja@example.com"}')
printf '%s' "$lookup" | python3 -c 'import json,sys; v=json.load(sys.stdin); assert v["recipientMaskedName"]=="L*** E***", v; assert v["maskedKey"]=="lo***@example.com", v; print("OK: busca Pix simulada")'

echo "Smoke test OK - API Pix em $API"
