# Termex Play Store Pack

This folder holds the repeatable 1.0 Play Store pack for Termex-Android.

Contents:

- `assets/icon/play-icon-512.png`
- `assets/feature-graphic.png`
- `assets/phone/` phone screenshots
- `assets/alt-text.md`
- `metadata/store-listing.md`
- `metadata/release-notes-v1.txt`
- `metadata/data-safety-worksheet.md`
- `metadata/content-rating-notes.md`
- `metadata/reviewer-notes.md`
- `official-requirements.md`

Build the pack:

```bash
source /Users/bolyki/dev/source/build-env.sh
./scripts/prepare_play_store_pack.sh
```

The script also copies the final pack, AAB, mapping file, and `.icon` source to `~/Desktop/Termex-a`.
