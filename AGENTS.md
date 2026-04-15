# AGENTS.md

Read in this order:

- [Root AGENTS](/Users/bolyki/dev/source/AGENTS.md)
- [Agent index](/Users/bolyki/dev/source/AGENT_INDEX.md)
- [README](./README.md)
- [SECURITY.md](./SECURITY.md)
- [REPO_IMPROVEMENT_PLAN.md](./REPO_IMPROVEMENT_PLAN.md)

Rules:

- Source `/Users/bolyki/dev/source/build-env.sh` before Gradle work.
- Shared Android artifacts and caches live under `~/dev/library`.
- Keep generated Gradle output out of the source tree.
- Treat signing and key rotation files as operational docs, not product docs.
- Use `~/dev/creds/` for auth and signing references.
