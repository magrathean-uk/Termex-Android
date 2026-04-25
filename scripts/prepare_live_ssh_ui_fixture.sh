#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Prepare Termex live SSH UI fixture exports from the desktop VM bundle.

Defaults:
  TERMEX_VM_FIXTURE_ROOT=/Users/bolyki/dev/test/termex-vms
  TERMEX_VM_NAME=opensuse

Run:
  TERMEX_VM_NAME=debian bash scripts/prepare_live_ssh_ui_fixture.sh
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=live_vm_harness.sh
source "$SCRIPT_DIR/live_vm_harness.sh"

ROOT="$(live_vm_fixture_root)"
VM_NAME="${TERMEX_VM_NAME:-opensuse}"
LOGIN_JSON="$(live_vm_login_json "$ROOT")"

if [[ ! -f "$LOGIN_JSON" ]]; then
  echo "missing fixture file: $LOGIN_JSON" >&2
  exit 2
fi

python3 - "$LOGIN_JSON" "$ROOT" "$VM_NAME" <<'PY'
import json
import pathlib
import shlex
import sys

login_json = pathlib.Path(sys.argv[1])
root = pathlib.Path(sys.argv[2])
vm_name = sys.argv[3]

with login_json.open(encoding="utf-8") as f:
    matrix = json.load(f)

if vm_name not in matrix:
    raise SystemExit(f"missing VM in fixture bundle: {vm_name}")

item = matrix[vm_name]
password = item["password_logins"][1]
root_password = item["password_logins"][0]
cert = item["cert_logins"][0]
key = item["cert_logins"][1]

def read(path: str) -> str:
    return pathlib.Path(path).read_text(encoding="utf-8")

values = {
    "TERMEX_VM_FIXTURE_ROOT": str(root),
    "TERMEX_VM_NAME": vm_name,
    "TERMEX_UI_TEST_LIVE_HOST": item["host"],
    "TERMEX_UI_TEST_LIVE_PORT": str(item["port"]),
    "TERMEX_UI_TEST_LIVE_USERNAME": key["username"],
    "TERMEX_UI_TEST_LIVE_NAME": f"{item['name']} key",
    "TERMEX_UI_TEST_KEY_NAME": f"{vm_name}-user3",
    "TERMEX_UI_TEST_KEY_TEXT": read(key["key"]),
    "TERMEX_UI_TEST_PASSWORD_HOST": item["host"],
    "TERMEX_UI_TEST_PASSWORD_PORT": str(item["port"]),
    "TERMEX_UI_TEST_PASSWORD_USERNAME": password["username"],
    "TERMEX_UI_TEST_PASSWORD": password["password"],
    "TERMEX_UI_TEST_PASSWORD_NAME_FOR_SERVER": f"{item['name']} password",
    "TERMEX_UI_TEST_CERT_HOST": item["host"],
    "TERMEX_UI_TEST_CERT_PORT": str(item["port"]),
    "TERMEX_UI_TEST_CERT_USERNAME": cert["username"],
    "TERMEX_UI_TEST_CERT_NAME_FOR_SERVER": f"{item['name']} cert",
    "TERMEX_UI_TEST_CERT_KEY_NAME": f"{vm_name}-user2",
    "TERMEX_UI_TEST_CERT_KEY_TEXT": read(cert["key"]),
    "TERMEX_UI_TEST_CERT_NAME": f"{vm_name}-user2-cert",
    "TERMEX_UI_TEST_CERT_TEXT": read(cert["cert"]),
    "TERMEX_UI_TEST_JUMP_HOST": item["host"],
    "TERMEX_UI_TEST_JUMP_PORT": str(item["port"]),
    "TERMEX_UI_TEST_JUMP_USERNAME": key["username"],
    "TERMEX_UI_TEST_JUMP_NAME": f"{item['name']} jump",
    "TERMEX_UI_TEST_JUMP_TARGET_HOST": "127.0.0.1",
    "TERMEX_UI_TEST_JUMP_TARGET_PORT": str(item["guest_ssh_port"]),
    "TERMEX_VM_KEY_PATH": key["key"],
    "TERMEX_VM_KEY_USERNAME": key["username"],
    "TERMEX_VM_ROOT_PASSWORD": root_password["password"],
}

for name, value in values.items():
    print(f"export {name}={shlex.quote(value)}")
PY
