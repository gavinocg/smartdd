#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# SMARTDD - Deploy Script
# Uso: sudo ./scripts/deploy.sh [--skip-build] [--skip-migrate]
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

echo "=== SMARTDD Deploy ==="

# 1. Pull latest code
echo "[1/6] Pulling latest code..."
git pull origin main

# 2. Install backend dependencies
echo "[2/6] Installing backend dependencies..."
cd backend
npm ci --omit=dev
npx prisma generate

# 3. Run database migrations
if [[ "${1:-}" != "--skip-migrate" ]]; then
  echo "[3/6] Running Prisma migrations..."
  npx prisma migrate deploy
  echo "[3/6] Running seed..."
  npx tsx src/seed.ts
else
  echo "[3/6] SKIP: migrations"
fi

# 4. Build backend
if [[ "${1:-}" != "--skip-build" ]]; then
  echo "[4/6] Building backend (TypeScript)..."
  npm run build
else
  echo "[4/6] SKIP: build"
fi
cd ..

# 5. Restart containers
echo "[5/6] Restarting Docker containers..."
cd infra
docker compose down --remove-orphans
docker compose up -d --build
cd ..

# 6. Verify
echo "[6/6] Verifying deployment..."
sleep 3
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8000/api/v1/health || echo "000")
if [ "$HEALTH" = "200" ]; then
  echo "=== Deploy OK (health check: $HEALTH) ==="
else
  echo "=== WARNING: Health check returned $HEALTH ==="
fi
