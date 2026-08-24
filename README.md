# #52GameChallenge — Android tracker

Sideloaded Kotlin + Jetpack Compose app for tracking beaten games.
Built by GitHub Actions — no local Android tooling needed.

## Building & updating

1. Repo root holds `settings.gradle.kts`, `app/`, `keystore/`, and
   `.github/workflows/android-apk.yml`.
2. Pushing (or Run workflow) builds an optimised **release** APK signed
   with the shared keystore in `keystore/debug.keystore`, so every build
   installs over the previous one — no uninstalling.
3. Download the `52GameChallenge-release-apk` artifact from the Actions
   run, unzip, sideload `app-release.apk`.

## First run on the phone

1. Copy the renamed `52GameChallenge` folder to the phone (year folders
   2015..2026 + `52gc-import.csv` at its root). A Syncthing-synced copy
   works great.
2. Settings (☰ menu) → Choose folder → pick it → Import now.
3. Optional: drop replacement platform icons in `platform-icons/`
   inside that folder as `<slug>.png` (e.g. `nintendo-switch.png`) —
   no rebuild needed. 44 icons ship built-in; anything else gets an
   initials tile.

## What the app does (v0.7)

- Top nav bar everywhere: ☰ full-screen menu (Homepage / Completed /
  Now Playing / Stats / Settings), centred logo, + straight to Add.
  Hides on scroll down, returns on scroll up.
- Home: Have I beaten this? instant fuzzy search · Now Playing (two
  cards + full page, "Beaten!" prefills the Add form) · clickable
  stat cards with pace line · Browse by year grid · top platforms.
- Completed: newest/oldest/A–Z, year + console filters with Clear,
  list / grid / large-grid views, year dividers, collage thumbnails.
- Game page: collage, details, notes card, edit everything, replay
  flag, replace image from gallery (old one auto-archived), and the
  collage builder: 3 photos + logo tile, pinch/zoom/rotate/pan or
  letterbox per tile, saves a 2048×2048 JPEG named into the year
  folder.
- Add beaten: duplicate warning, platform autocomplete, auto N/52,
  attach a ready-made collage from the gallery (renamed on save).
- Stats: games-per-year chart, era breakdown, editable series counts,
  fun facts, clickable platform table.
- Settings: data folder picker (SAF), CSV import, timestamped CSV
  export to `exports/`.

## Data files

- `52gc-import.csv` — seed/import format (Year, Number, Name, Console,
  Date, ImageFile, Notes). Exports round-trip through the same format.
- Collages: `<year>/<year>-<nnn> - <Name> (<Platform>).jpg`
- Replaced images land in `archive/`, exports in `exports/`.
