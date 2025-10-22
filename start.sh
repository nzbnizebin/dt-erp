#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Apache Maven is required but not found in PATH" >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "Node.js (npm) is required but not found in PATH" >&2
  exit 1
fi

cd "$BACKEND_DIR"
if [ ! -d "$ROOT_DIR/data" ]; then
  mkdir -p "$ROOT_DIR/data"
fi

echo "Installing backend dependencies..."
mvn -q dependency:go-offline

echo "Starting backend..."
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=default" &
BACKEND_PID=$!

cleanup() {
  echo "Stopping backend (PID $BACKEND_PID)..."
  kill "$BACKEND_PID" >/dev/null 2>&1 || true
}

trap cleanup EXIT

cd "$FRONTEND_DIR"
echo "Installing frontend dependencies..."
npm install

echo "Starting frontend dev server..."
npm run dev -- --host
