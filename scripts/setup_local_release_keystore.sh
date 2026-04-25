#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST_DIR="${TERMEX_RELEASE_KEY_DIR:-$HOME/dev/creds}"
DEST_KEYSTORE="${TERMEX_RELEASE_KEYSTORE_PATH:-$DEST_DIR/termex-android-upload.jks}"
KEY_ALIAS="${TERMEX_RELEASE_KEY_ALIAS:-termex-upload}"
KEYSTORE_PROPERTIES_FILE="${TERMEX_KEYSTORE_PROPERTIES_FILE:-$ROOT/keystore.properties}"
STORE_PASSWORD="${TERMEX_RELEASE_STORE_PASSWORD:-}"
KEY_PASSWORD="${TERMEX_RELEASE_KEY_PASSWORD:-}"
DNAME="${TERMEX_RELEASE_DNAME:-CN=Termex Android, OU=Mobile, O=Magrathean UK Ltd, L=Watford, S=Hertfordshire, C=GB}"

random_secret() {
  python3 - <<'PY'
import secrets
print(secrets.token_urlsafe(24))
PY
}

if [[ -z "$STORE_PASSWORD" ]]; then
  STORE_PASSWORD="$(random_secret)"
fi

if [[ -z "$KEY_PASSWORD" ]]; then
  KEY_PASSWORD="$STORE_PASSWORD"
fi

mkdir -p "$DEST_DIR"

if [[ ! -f "$DEST_KEYSTORE" ]]; then
  keytool -genkeypair \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 3650 \
    -keystore "$DEST_KEYSTORE" \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "$DNAME"
fi

cat > "$KEYSTORE_PROPERTIES_FILE" <<EOF
# Local-only release signing config for Termex Android.
# Keep this file untracked.
RELEASE_STORE_FILE=$DEST_KEYSTORE
RELEASE_STORE_PASSWORD=$STORE_PASSWORD
RELEASE_KEY_ALIAS=$KEY_ALIAS
RELEASE_KEY_PASSWORD=$KEY_PASSWORD
EOF

chmod 600 "$KEYSTORE_PROPERTIES_FILE" "$DEST_KEYSTORE" 2>/dev/null || true

echo "Local release signing prepared."
echo "Keystore: $DEST_KEYSTORE"
echo "Properties: $KEYSTORE_PROPERTIES_FILE"
