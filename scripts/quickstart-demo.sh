#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v openssl >/dev/null || { echo 'Instale openssl.' >&2; exit 1; }
if [ ! -f .env ]; then
  { printf 'JWT_SECRET_BASE64=%s\n' "$(openssl rand -base64 32)"
    printf 'PIX_LOOKUP_SECRET_BASE64=%s\n' "$(openssl rand -base64 32)"
    printf 'JWT_EXPIRATION_MINUTES=60\nSPRING_PROFILES_ACTIVE=demo\n'
  } > .env
  chmod 600 .env
fi
if command -v podman-compose >/dev/null 2>&1; then
  podman-compose -f compose.yaml up --build -d
elif command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  docker compose -f compose.yaml up -d --build
elif command -v docker-compose >/dev/null 2>&1 && ! command -v podman >/dev/null 2>&1; then
  docker-compose -f compose.yaml up -d --build
else
  echo 'Instale podman-compose (recomendado no Podman) ou Docker Compose V2.' >&2
  exit 1
fi
printf '\nSwagger: http://localhost:8083/swagger-ui/index.html\n'
printf 'Login demo: demo@arqvora.local / Demo@123456\n'
printf 'Teste automatico: ./scripts/smoke-test-demo.sh\n'
