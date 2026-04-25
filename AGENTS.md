# AGENTS.md

Read in this order:

- [Root AGENTS](/Users/bolyki/dev/source/AGENTS.md)
- [Agent index](/Users/bolyki/dev/source/AGENT_INDEX.md)
- [README](./README.md)
- [Runbook](./RUNBOOK.md)
- [app build file](./app/build.gradle.kts)

Rules:

- Source `/Users/bolyki/dev/source/build-env.sh` before Gradle work.
- Keep generated Gradle output out of the repo surface.
- Keep `keystore.properties`, release keys, local properties, and VM fixtures local only.
- Preserve the four build variants and the `com.termex.app.testing.TermexHiltTestRunner` test entrypoint.
- Keep host-key verification, redacted diagnostics, and local-only session restore intact unless the change explicitly targets them.
- The app is free/open source. Do not reintroduce paywall, billing, Sentry, or personal-account service config without an explicit product decision.
