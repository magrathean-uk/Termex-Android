# Google Play Store Subscription Setup for Termex Pro

## Subscription Configuration

### Product ID
```
termex_pro_subscription
```

This ID is configured in `SubscriptionManager.kt` and must match exactly in Google Play Console.

---

## Step-by-Step Setup in Google Play Console

### 1. Navigate to Subscription Settings
1. Open [Google Play Console](https://play.google.com/console)
2. Select your app: **Termex**
3. Go to **Monetize** → **Subscriptions**
4. Click **Create subscription**

### 2. Create the Subscription Product

| Field | Value |
|-------|-------|
| **Product ID** | `termex_pro_subscription` |
| **Name** | Termex Pro |
| **Description** | Professional SSH terminal with unlimited connections, key management, snippets, and more. |

### 3. Add Base Plan

Click **Add base plan** and configure:

| Field | Value |
|-------|-------|
| **Base plan ID** | `monthly` |
| **Renewal type** | Auto-renewing |
| **Billing period** | 1 month |

### 4. Add Free Trial Offer

Click **Add offer** → **Free trial offer**:

| Field | Value |
|-------|-------|
| **Offer ID** | `monthly-freetrial` |
| **Eligibility** | New customer acquisition |
| **Phases** | |
| └ Phase 1 (Free trial) | **P7D** (7 days), Price: **Free** |
| └ Phase 2 (Paid) | Price: **$9.99 USD** |

### 5. Regional Pricing (Auto-calculated)

Google will auto-convert $9.99 USD to local currencies. Review and adjust if needed:

| Region | Suggested Price |
|--------|-----------------|
| United States | $9.99 |
| Euro zone | €9.99 |
| United Kingdom | £7.99 |
| Canada | $12.99 CAD |
| Australia | $14.99 AUD |
| Japan | ¥1,500 |

### 6. Subscription Benefits (for Play Store listing)

Add these benefits in the subscription configuration:

- ✓ Unlimited server connections
- ✓ SSH key generation & import
- ✓ Command snippets library
- ✓ Port forwarding (Local, Remote, Dynamic)
- ✓ Multi-terminal workplaces
- ✓ Jump host / bastion support
- ✓ Host key verification & security

---

## Grace Period & Account Hold Settings

Navigate to **Monetize** → **Subscriptions** → **Grace period**

| Setting | Recommended Value |
|---------|-------------------|
| **Grace period** | 7 days |
| **Account hold** | 30 days |
| **Resubscribe** | Enabled |

---

## Testing Configuration

### 1. License Testing
1. Go to **Setup** → **License testing**
2. Add test email addresses
3. These accounts can subscribe without being charged

### 2. Subscription Testing Tracks
- Use **Internal testing** track for development
- Subscriptions renew quickly in test mode (daily instead of monthly)

---

## App Configuration Reference

### SubscriptionManager.kt
```kotlin
companion object {
    const val PRODUCT_ID = "termex_pro_subscription"
}
```

### Build Variants

| Build Type | BYPASS_PAYWALL | Purpose |
|------------|----------------|---------|
| `debug` | `false` | Standard development (paywall enforced) |
| `release` | `false` | Production (paywall enforced) |
| `dev` | `true` | Development testing (paywall bypassed) |

---

## Subscription Flow Summary

```
┌─────────────────┐
│   App Launch    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Onboarding     │ (First time only)
│  4 pages        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│    Paywall      │────▶│  Google Play    │
│  (Back blocked) │     │  Billing Flow   │
└────────┬────────┘     └────────┬────────┘
         │                       │
         │    ┌──────────────────┘
         │    │
         ▼    ▼
┌─────────────────┐
│   SUBSCRIBED    │
│   Main App      │
└─────────────────┘
```

---

## Verification Checklist

- [ ] Product ID matches: `termex_pro_subscription`
- [ ] 7-day free trial configured
- [ ] Price set to $9.99/month
- [ ] Grace period enabled (7 days)
- [ ] License testers added
- [ ] Subscription activated (not draft)
- [ ] App published to at least internal testing track

---

## Important Notes

1. **Subscription must be activated** - Drafts won't work
2. **App must be published** - Even internal track works
3. **First purchase sync** - May take up to 24 hours for billing to work
4. **Test accounts** - Use license testing for development
5. **Demo mode** - Only available via hidden activation (5 taps on logo during onboarding)

---

## Support

The paywall:
- ✓ Blocks back button navigation
- ✓ Shows on LOADING, ERROR, and NOT_SUBSCRIBED states
- ✓ Displays 7-day free trial prominently
- ✓ Falls back to "$9.99/month" if Play Store unavailable
- ✓ Has premium visual design with animations
