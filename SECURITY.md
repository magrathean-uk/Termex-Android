# Security

This project handles SSH credentials and must be treated as sensitive software.

## Reporting
If you discover a vulnerability or secret leakage, stop and notify the maintainers immediately.
Do not open public issues with secrets.

## Secret handling
- Never commit secrets, keys, or credentials.
- Use `keystore.properties` (ignored) or environment variables for release signing.
- Store user passwords only in encrypted storage (see `SecurePasswordStore`).

## Release signing
- Keystore files and passwords must be stored in CI secrets.
- Local dev uses `keystore.properties` (ignored by git).

## Cleanup expectations
- `local.properties` should be generated locally by Android Studio and never committed.
- Run a secrets scan before release.
