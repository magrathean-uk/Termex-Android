# Data Safety Worksheet

Review this before filling the Play Console form.

Known app behavior:

- No ads.
- No analytics SDK.
- No third-party data sharing by the app.
- Billing uses Google Play Billing for subscription purchase state.
- User-entered SSH credentials, keys, certificates, and host trust data stay on-device.
- Android backup is enabled for non-secret app metadata only.
- Terminal session restore is local-only and excluded from backup.

Likely declarations to verify:

- Collected by developer: none.
- Shared with third parties by developer: none.
- Security practices:
  - data encrypted in transit for app network calls
  - user can request account/data deletion only if future account features are added; current app has no in-app account system

Manual review still needed:

- Check final SDK inventory in the Play Console scanner.
- Confirm Billing-only Google SDK usage does not change declarations.
- Confirm website privacy page language matches final declarations.
