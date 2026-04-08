# Security

## Sensitive data

- SSH private keys, passphrases, and auth tokens stay out of persisted app models.
- Host keys are validated before trust is accepted.
- Signing credentials should stay in local developer-only configuration.

## Repo rules

- Keep `local.properties` and release keystores out of source control.
- Keep GitHub auth and App Store Connect material in `~/dev/creds/`, not in the repo tree.
- Keep example files, templates, and setup guidance in the repo.
- Do not store plaintext secrets in docs or log output.
