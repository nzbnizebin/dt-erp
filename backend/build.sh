#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
SRC_DIR="$ROOT_DIR/src/main/java"
TARGET_DIR="$ROOT_DIR/target"
CLASSES_DIR="$TARGET_DIR/classes"

mkdir -p "$CLASSES_DIR"
find "$CLASSES_DIR" -type f -name '*.class' -delete

mapfile -t SOURCES < <(find "$SRC_DIR" -name '*.java')
if [ "${#SOURCES[@]}" -eq 0 ]; then
  echo "No source files found" >&2
  exit 1
fi

javac -encoding UTF-8 --release 17 -d "$CLASSES_DIR" "${SOURCES[@]}"

jar --create --file "$TARGET_DIR/attendance-backend.jar" --main-class com.example.attendance.AttendanceApplication \
  -C "$CLASSES_DIR" .

echo "Built $TARGET_DIR/attendance-backend.jar"
