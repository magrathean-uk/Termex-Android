# Google Play subscription setup

This doc is the 1.0 Play billing source of truth for Termex-Android.

## Product contract

- Package id: `com.termex.app`
- Billing product id: `termex_pro_subscription`
- Product type: auto-renewing subscription
- Launch catalog:
  - yearly only
  - USD `$4.99`
  - `7-day` free trial

## Public listing links

- Privacy policy: `https://magrathean.uk/apps/termex/privacy`
- Website/support target: `https://magrathean.uk/apps/termex`
- Support email: `[email protected]`

## Credentials and secrets

- Keep upload keys and Play credentials outside the repo.
- Use `~/dev/creds/` for local signing material.
- Use untracked `keystore.properties` in the repo root to point at the upload key.
- Raw Google API keys are not the publish credential for Play.
- If Play Developer API automation is added later, use Play OAuth or a service account with `androidpublisher`, not a repo-tracked API key.

## Play Console setup

1. Confirm or create the Play app for `com.termex.app`.
2. Set the app category to `Tools`.
3. Mark the app as:
   - no ads
   - no analytics SDK
   - no third-party data sharing
4. Create the subscription `termex_pro_subscription`.
5. Add one yearly base plan.
6. Set the price to USD `$4.99`.
7. Add one introductory offer:
   - free trial
   - `7 days`
8. Add internal-test users and license testers before proof.

## Required proof before production

Use a Play-served build from internal testing. Fake billing proof is not enough for 1.0.

Required flow:

1. Fresh install, unsubscribed user sees paywall after onboarding.
2. Product details load with yearly price and `7-day` trial copy.
3. Purchase succeeds.
4. Purchase is acknowledged.
5. Subscribed user reaches main shell.
6. Reinstall or restore returns subscription correctly.
7. `dev` still bypasses paywall. `release` does not.

## Store metadata checklist

- English-only listing is fine for 1.0.
- Listing claims must match the shipped build:
  - biometric lock
  - SSH keys and certificates
  - host-key verification
  - jump host
  - snippets
  - workplaces
  - local, remote, and dynamic forwarding
  - persistent tmux restore
- Do not claim hidden or unproved features.
