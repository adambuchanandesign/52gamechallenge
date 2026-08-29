# #52GameChallenge — Android tracker (Claude AI)

A personal, sideloaded Android app for the #52GameChallenge: beat a game a
week, log it, collage it. Eleven years of a spreadsheet, now with a proper
home. Built with Kotlin + Jetpack Compose + Room, compiled entirely by
GitHub Actions — no local Android tooling ever needed.

<img width="260" alt="Screenshot_20260829-141840" src="https://github.com/user-attachments/assets/92db6104-4bf5-4722-99aa-d6839b95cb35" /> <img width="260" alt="Screenshot_20260829-141905" src="https://github.com/user-attachments/assets/6a0e6873-5e94-4b32-a4f0-2a54d471b381" /> <img width="260" alt="Screenshot_20260829-142006" src="https://github.com/user-attachments/assets/f8ab6e02-8aed-4c62-8e4b-df988c06707a" />

---

## What it does

- **Tracks beaten games** — 733 and counting — with year, N/52 sequence,
  platform, date, notes, replay flag, and the square collage image for
  each one.
- **Now Playing & Backlog** — what's on the go and what's queued, each
  with cover art, notes, and their own pages. Backlog entries promote to
  Now Playing in one tap; Now Playing entries can log straight into the
  beaten list, prefilled.
- **Live IGDB search** — type in "Have I beaten this?" and get instant
  local answers plus live IGDB results with covers; pick one to add it
  with the right name, platforms and art.
- **Random picker** 🎲 — filter by genre and/or era, spin, get a game
  with art, screenshots and summary; add it to Now Playing or Backlog,
  or roll again. Tells you if you already beat it.
- **Collage builder** — recreate the classic 4-tile collage (title
  screen / final area / ending / logo) from photos: tap a tile, pick a
  photo, pinch/zoom/pan/twist, straighten or rotate 90°, save a
  2048×2048 JPEG named correctly into the year folder.
- **Stats** — games per year, eras, genres and release decades (bar or
  pie), platform table, streaks, droughts, milestones, on-this-day
  history, editable series counts, and a pile of fun facts.
- **Backups** — automatic JSON snapshots after every change (newest 5
  kept), one-tap full backup, old-school .xlsx spreadsheet export, CSV
  export/import, and full restore with a confirmation preview.

---

## First-time setup, from scratch

### 1. Build the APK (GitHub Actions)

1. Create a GitHub repository and upload this project's contents:
   `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
   the `app/` folder, the `keystore/` folder, and
   `.github/workflows/android-apk.yml`.
2. Every push (or Actions → Build APK → Run workflow) builds a signed
   **release** APK. The signing key is the committed
   `keystore/debug.keystore`, so every build installs *over the top* of
   the previous one — settings and data survive updates.
3. Download the `52GameChallenge-release-apk` artifact from the run,
   unzip it, copy `app-release.apk` to the phone and install it
   (allow "install from unknown sources" the first time).

### 2. The data folder

The app reads and writes everything inside one folder you own —
recommended: a Syncthing-synced folder so images and backups are
mirrored off the phone automatically.

Layout:

```
52GameChallenge/
├── 52gc-import.csv          ← seed data (optional, first import)
├── 2015/ … 2026/            ← one folder per year of collage images
│   └── 2015-001 - Game Name (Platform).jpg
├── platform-icons/          ← optional custom icons: <slug>.png
├── archive/                 ← replaced images land here (auto)
└── exports/                 ← backups + exports land here (auto)
```

In the app: **☰ → Settings → Choose folder** → pick the folder →
grant access. Then **Import now** if you have a `52gc-import.csv`
(columns: Year, Number, Name, Console, Date, ImageFile, Notes).

### 3. IGDB credentials (free, optional but worth it)

Powers live search, covers, the random picker, About panels, and
enrichment. Free, no card, no billing — the only limit is 4 requests
per second, which normal use never hits.

1. Create/log into a Twitch account and enable two-factor auth.
2. Go to **dev.twitch.tv** → Your Console → Applications →
   **Register Your Application**.
3. Name: anything. OAuth Redirect URL: `http://localhost`.
   Category: Application Integration.
4. Open the app you created → copy the **Client ID** → click
   **New Secret** → copy that too.
5. In this app: **Settings → IGDB live search** → paste both →
   **Save & test** → look for "Connected!".

### 4. Enrichment (one-off, recommended)

**Settings → IGDB enrichment → Run enrichment.** One pass fetches
genres, release year, rating, cover and summary for every beaten game
(a few minutes for ~700 games; pausable and resumable — it only ever
processes games it hasn't done). Unlocks the genre and release-decade
charts and extra fun facts. Games added later enrich themselves
automatically.

---

## Using it day to day

- **Beat a game?** Tap **+** in the top bar (or Add beaten / Add Game
  in the menu). Duplicate names warn you. Attach a collage from the
  gallery, or save first and use **Build collage** on the game's page.
- **Started a game?** Home → Now playing → **+ Add** → search → pick
  the IGDB match (or add manually) → platform chips + validation keep
  your platform names consistent → **Start playing**.
- **Someone recommends something?** Same flow into the **Backlog**.
- **Can't decide?** **Random** in the menu. Spin. Argue with the
  result. Roll again.
- **"Have I beaten…?"** — the Home search answers instantly from your
  list, and its IGDB block shows the whole series with beaten-checks.
- **Collage builder** — game page → Build collage. Tap an empty tile
  to pick its photo (always shown whole, letterboxed); pinch/drag/twist
  to frame it; **Straighten** zeroes accidental rotation, **Rotate 90°**
  turns it, **Reset** starts the tile over. Order: 1 = title screen,
  2 = final area, 3 = ending; the logo fills tile 4. Save writes the
  2048×2048 JPEG into the right year folder (replacing an old image
  archives it first).
- **Browsing** — Completed shows the collage wall (grid by default;
  list and large views available), filterable by year/platform/sort,
  with year dividers.

## Backups & exports (Settings)

- **Auto-backup**: after any data change, a JSON snapshot is written to
  `exports/` (~8s later, batched); the newest 5 are kept. With the
  folder in Syncthing this is an automatic offsite backup.
- **Backup now**: full timestamped JSON — every beaten game including
  enrichment, Now Playing, Backlog, series list. Credentials are never
  included.
- **Restore from backup…**: pick a backup JSON; you'll see exactly
  what's in it and confirm before anything is replaced.
- **Spreadsheet**: a real .xlsx with List / Summary / Now Playing /
  Backlog sheets — the classic spreadsheet, reborn.
- **Export now (CSV)**: the same 7-column format the import reads, for
  round-tripping.

## Updating the app

Edit or re-upload files in the repo → Actions builds → sideload the
new `app-release.apk` over the old one. Data, settings, credentials and
the folder grant all persist. Database changes migrate automatically.

## Platform icons

44 ship built-in. To override or add: drop `<slug>.png` into
`platform-icons/` in the data folder — e.g. `nintendo-snes.png`,
`sega-mega-drive.png`. Anything unknown gets an initials tile. No
rebuild needed; restart the app to clear its icon cache.
