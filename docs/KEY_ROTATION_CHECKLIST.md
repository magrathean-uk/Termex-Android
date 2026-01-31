# Key Rotation Checklist

Use this whenever a signing key or API key is suspected to be exposed.

## 1) Android signing key (upload key)
- [ ] Generate a new upload keystore.
- [ ] Update Play Console with the new upload key.
- [ ] Revoke/retire the old upload key.
- [ ] Update CI secrets for `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- [ ] Verify the release build signs with the new key.

## 2) Google APIs / external keys
- [ ] Regenerate the key in the provider console.
- [ ] Restrict by package name + SHA-256 signing cert fingerprint.
- [ ] Update CI/runtime config as needed.
- [ ] Monitor usage for anomalies.

## 3) SSH / server credentials
- [ ] Change server passwords and/or rotate SSH keys.
- [ ] Update any stored credentials in the app (encrypted store).
- [ ] Invalidate old keys on servers.

## 4) Repo hygiene after rotation
- [ ] Purge secrets from git history.
- [ ] Force-push rewritten history.
- [ ] Re-clone the repo on all dev machines.
- [ ] Run a secrets scan to confirm clean state.
