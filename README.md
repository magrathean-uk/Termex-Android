# Termex-Android

Termex-Android is the Android version of Termex: a Compose-based SSH client with onboarding, subscription gating, biometric lock, SSH keys and certificates, host-key verification, port forwarding, snippets, workspaces, and terminal sessions.

## Canonical docs

- [AI guide](./AI.md)
- [Security](./SECURITY.md)
- [Google Play subscription setup](./GOOGLE_PLAY_SUBSCRIPTION_SETUP.md)
- [Repo improvement plan](./REPO_IMPROVEMENT_PLAN.md)

## Layout

- `app/` - Android application source
- `docs/` - operational and key management notes
- `gradle/` - wrapper and build tooling

## Notes

- Keep generated Gradle output out of the source tree.
- Treat signing and key rotation files as operational, not product docs.
- GitHub auth uses `~/dev/creds/git.key`; App Store Connect uploads use `~/dev/creds/AuthKey_96SRQR4URV.p8` and `~/dev/creds/asc.md`.
