# Security

## Sensitive data

- SSH private keys, passphrases, and auth tokens stay out of persisted app models.
- Host keys are validated before trust is accepted. Unknown and changed fingerprints are rejected until the user explicitly trusts them.
- Diagnostic logs are redacted before persistence or sharing to avoid leaking passwords, tokens, PEM blocks, or credential-bearing URLs.
- Persistent session restore stays local on device. It is not synced or exported.
- Signing credentials should stay in local developer-only configuration.

## Repo rules

- Keep `local.properties` and release keystores out of source control.
- Keep GitHub auth and App Store Connect material in `~/dev/creds/`, not in the repo tree.
- Keep live VM UI fixture bundles and exported SSH creds local only, outside the repo tree. The default fixture root is `/Users/bolyki/dev/test/termex-vms`.
- Keep example files, templates, and setup guidance in the repo.
- Do not store plaintext secrets in docs or log output.
- Supported Android floor is Android 10+ (`minSdk 29`).
- Session restore stays local. The dedicated session database is excluded from cloud backup and device transfer.
