#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_PROPERTIES_FILE="${TERMEX_KEYSTORE_PROPERTIES_FILE:-$ROOT/keystore.properties}"
REQUIRED_RELEASE_SIGNING_PROPS=(
  RELEASE_STORE_FILE
  RELEASE_STORE_PASSWORD
  RELEASE_KEY_ALIAS
  RELEASE_KEY_PASSWORD
)

resolve_release_signing_prop() {
  local name="$1"
  local from_file=""

  if [[ -f "$KEYSTORE_PROPERTIES_FILE" ]]; then
    from_file="$(
      python3 - "$KEYSTORE_PROPERTIES_FILE" "$name" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
target = sys.argv[2]

for raw_line in path.read_text(encoding="utf-8").splitlines():
    line = raw_line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    key, value = line.split("=", 1)
    if key.strip() == target:
        print(value.strip())
        break
PY
    )"
  fi

  if [[ -n "$from_file" ]]; then
    printf '%s\n' "$from_file"
    return 0
  fi

  local from_env="${!name:-}"
  if [[ -n "$from_env" ]]; then
    printf '%s\n' "$from_env"
  fi
}

check_release_signing() {
  local missing=()
  local store_file
  store_file="$(resolve_release_signing_prop RELEASE_STORE_FILE)"

  for key in "${REQUIRED_RELEASE_SIGNING_PROPS[@]}"; do
    if [[ -z "$(resolve_release_signing_prop "$key")" ]]; then
      missing+=("$key")
    fi
  done

  if [[ -n "$store_file" && ! -f "$store_file" ]]; then
    missing+=("RELEASE_STORE_FILE (file not found: $store_file)")
  fi

  if (( ${#missing[@]} > 0 )); then
    {
      echo "Release signing is not configured."
      echo "Provide keystore.properties or these environment variables:"
      for key in "${REQUIRED_RELEASE_SIGNING_PROPS[@]}"; do
        echo "- $key"
      done
      echo "Missing or invalid:"
      for item in "${missing[@]}"; do
        echo "- $item"
      done
    } >&2
    return 1
  fi
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  check_release_signing
fi
