#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PACK_DIR="$ROOT_DIR/playstore/v1"
DESKTOP_DIR="$HOME/Desktop/Termex-a"

source /Users/bolyki/dev/source/build-env.sh >/dev/null 2>&1

mkdir -p "$PACK_DIR/assets/phone" "$PACK_DIR/metadata"

"$ROOT_DIR/scripts/capture_play_store_screenshots.sh" "$PACK_DIR/assets/phone"
./gradlew bundleRelease
python3 "$ROOT_DIR/scripts/build_play_store_pack.py" --repo-root "$ROOT_DIR" --pack-dir "$PACK_DIR"

rm -rf "$DESKTOP_DIR"
mkdir -p "$DESKTOP_DIR"
cp -R "$PACK_DIR/." "$DESKTOP_DIR/"
cp "$ROOT_DIR/PLAY_RELEASE_RUNBOOK.md" "$DESKTOP_DIR/"
cp "$ROOT_DIR/GOOGLE_PLAY_SUBSCRIPTION_SETUP.md" "$DESKTOP_DIR/"
cp -R "$ROOT_DIR/termex.icon" "$DESKTOP_DIR/"

AAB_PATH="$(find "$ROOT_DIR/app/build/outputs/bundle/release" -name '*.aab' -print -quit)"
if [[ -n "$AAB_PATH" ]]; then
  mkdir -p "$DESKTOP_DIR/artifacts"
  cp "$AAB_PATH" "$DESKTOP_DIR/artifacts/Termex-1.0.0-release.aab"
fi

MAPPING_PATH="$ROOT_DIR/app/build/outputs/mapping/release/mapping.txt"
if [[ -f "$MAPPING_PATH" ]]; then
  mkdir -p "$DESKTOP_DIR/artifacts"
  cp "$MAPPING_PATH" "$DESKTOP_DIR/artifacts/mapping.txt"
fi
