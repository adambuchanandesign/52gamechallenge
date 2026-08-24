# 52 Game Challenge — Android tracker (Phase 1)

Sideloaded Kotlin + Jetpack Compose app for tracking beaten games.
Built by GitHub Actions — no local Android tooling needed.

## Build the APK (same flow as your previous apps)

1. Create a new GitHub repo (private is fine) and push the CONTENTS of this
   folder as the repo root (so `settings.gradle.kts` sits at the top level).
2. GitHub → Actions tab → the "Build APK" workflow runs on push
   (or run it manually via "Run workflow").
3. Download the `52GameChallenge-debug-apk` artifact, unzip, sideload
   `app-debug.apk`.

## First run on the phone

1. Copy your renamed `52GameChallenge` folder to the phone, e.g.
   `Pictures/52GameChallenge/` — containing the year folders (2015..2026)
   and `52gc-import.csv` at its root. A Syncthing-synced copy works great.
2. Open the app → Settings → **Choose folder** → pick that folder.
3. Tap **Import now** → "Imported 733 games".
4. Optional: create `platform-icons/` inside that folder and drop
   `<slug>.png` files there to override any platform icon without a rebuild
   (slug = lowercase name, non-alphanumerics as dashes, e.g.
   `nintendo-switch.png`, `commodore-amiga.png`). 44 icons are bundled;
   everything else gets a generated initials tile.

## Updating without uninstalling

The repo contains `keystore/debug.keystore` and the build signs every APK
with it, so updates install cleanly over the top. (The switch to this
keystore itself requires ONE final uninstall/reinstall.)

## What's in Phase 1

- Home: "Have I beaten this?" instant fuzzy search, totals, weekly pace
  tracker, top platforms
- Games: newest/oldest/A–Z, year + console filters, list and grid views
  (grid shows your collages)
- Game page: full collage, details, edit everything, replay flag, delete
- Add beaten game: live duplicate warning, platform suggestions,
  auto-incrementing N/52 counter
- Settings: folder picker (SAF — no Android/data prison), CSV import

## Phase 2 (next)

Collage builder (pinch/zoom/rotate per tile + logo, 2048x2048), stats page,
export (xlsx/CSV + images), libretro box art, platform icon manager UI,
ideas backlog.
