#!/usr/bin/env bash
#
# Starts or stops the YaFT backend the playground talks to.
#
#   scripts/backend.sh up     start it and wait until it answers
#   scripts/backend.sh down   stop it and drop the volume
set -euo pipefail

cd "$(dirname "$0")/.."

API_URL="${API_URL:-http://127.0.0.1:8080}"

case "${1:-up}" in
  up)
    docker compose up -d
    echo "waiting for ${API_URL}"
    # A missing key answering 404 means the router and the database are both
    # up; a health endpoint would be nicer but the API does not have one.
    for _ in $(seq 1 60); do
      if [ "$(curl -s -o /dev/null -w '%{http_code}' "${API_URL}/features/nothing" || true)" = "404" ]; then
        echo "backend ready"
        exit 0
      fi
      sleep 2
    done
    echo "error: backend did not become ready" >&2
    docker compose logs --tail 40 >&2
    exit 1
    ;;
  down)
    docker compose down --volumes
    ;;
  *)
    echo "usage: $0 [up|down]" >&2
    exit 1
    ;;
esac
