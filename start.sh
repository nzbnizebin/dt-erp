#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
DATA_DIR="$ROOT_DIR/data"

if ! command -v javac >/dev/null 2>&1; then
  echo "OpenJDK 17 javac is required but not found" >&2
  exit 1
fi

mkdir -p "$DATA_DIR"

echo "Building backend..."
"$BACKEND_DIR/build.sh"

echo "Starting backend..."
java -jar "$BACKEND_DIR/target/attendance-backend.jar" &
BACKEND_PID=$!

echo "Backend started with PID $BACKEND_PID"

cleanup() {
  echo "Stopping backend (PID $BACKEND_PID)..."
  kill "$BACKEND_PID" >/dev/null 2>&1 || true
}

trap cleanup EXIT

if command -v npm >/dev/null 2>&1; then
  cd "$FRONTEND_DIR"
  if [ -f package.json ]; then
    if [ ! -d node_modules ]; then
      echo "Installing frontend dependencies..."
      npm install
    fi
    echo "Starting frontend dev server..."
    npm run dev -- --host
  fi
else
  echo "npm not found; frontend server not started" >&2
  wait "$BACKEND_PID"
fi
