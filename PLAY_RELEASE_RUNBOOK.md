# Play release runbook

This is the short 1.0 release path for Termex-Android.

## 1. Local signing

Use one local upload key outside the repo.

Quick start:

```bash
source /Users/bolyki/dev/source/build-env.sh
./scripts/setup_local_release_keystore.sh
```

That creates:

- upload keystore under `~/dev/creds/`
- untracked `keystore.properties` in the repo root

## 2. Release-candidate proof

Run the full RC lane:

```bash
source /Users/bolyki/dev/source/build-env.sh
BUNDLETOOL_JAR=/absolute/path/to/bundletool-all.jar ./scripts/run_android_release_candidate.sh
```

This must finish before any Play upload.

## 3. Internal-test billing proof

Upload the signed `release` bundle to Play internal testing.

Required checks on a Play-served build:

1. Fresh install reaches paywall when unsubscribed.
2. Yearly plan shows USD `$4.99` with `7-day` trial.
3. Purchase succeeds.
4. App becomes subscribed.
5. Restore works after reinstall.

## 4. Production upload

After internal-test proof:

1. Confirm store listing, screenshots, feature graphic, privacy URL, support URL, and data safety answers.
2. Upload the same signed release line to production.
3. Complete content rating and app access declarations.
4. Submit for production review.

## 5. Rollback

If production issue appears:

- pause new rollout in Play Console
- ship a follow-up build from the same signing line
- if billing is affected, keep `dev` proof and `releaseProof` fake-billing proof green while the real Play fix is prepared
