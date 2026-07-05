# Changelog

All notable changes to TickLog are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — release hardening

### Fixed

- **Backup restore is now atomic.** Clearing and re-inserting happen inside a
  single database transaction, so a failed or interrupted restore rolls back and
  leaves existing data completely intact (previously a mid-restore failure could
  empty the database).
- **Writes no longer crash the app.** Checklist and preference writes handle
  `SQLite`/IO failures gracefully; a failed task toggle rolls the checkbox back
  and shows a message.

### Added

- **Signed release builds.** R8 shrinking/obfuscation and resource shrinking are
  enabled for release, with keep rules that keep backup/export working. Signing
  reads from a git-ignored `keystore.properties` (or CI env vars); see the README
  for generating a keystore and building a signed `.aab`.
- **Onboarding range limit** (up to ~2 years) with a clear message, preventing
  ANR/OOM from generating an unbounded number of days.

### Changed

- **Auto Backup disabled** (`allowBackup=false`). The app's data stays on the
  device — migration is handled by the built-in, user-controlled Backup &
  Restore — so the privacy promise holds and the Play Data Safety answers stay
  simple (no data collected, shared, or transferred off-device).
- Removed the unused Coil image-loading dependency.

## [1.0.0] — 2026-07-04

The first stable release of TickLog: a complete, offline-first daily checklist
recorder with a strictly black-and-white Material 3 design.

### Added

- **Home-screen widget** (Jetpack Glance). A glanceable, monochrome widget that
  shows today's date, completion percentage, the day's tasks and the current
  streak.
  - Adapts its layout across 2×2, 4×2 and 4×4 placements.
  - Tap a task to toggle it complete — instantly, without opening the app.
  - Tap the header to open the app; tap the streak to open Statistics.
  - Refreshes automatically on every change (in-app edits, widget toggles) and
    rolls over to the new day at midnight — no polling, battery-friendly.
- **Splash screen** via `androidx.core:core-splashscreen`: a minimal centred
  TickLog mark, white-on-black or black-on-white to match the system theme.
- **Themed launcher icon**: a monochrome adaptive icon with an Android 13+
  themed-icon (monochrome) layer.
- `LICENSE` (MIT), this `CHANGELOG.md`, and a full project `README.md`.

### Changed

- `MainActivity` is now `singleTop` and honours a deep-link extra so the widget's
  streak tap lands directly on the Statistics screen.

### Notes

- **No database schema change** — the widget reads the existing checklist data
  through the same repository the app uses; the store remains at version 2.
- Fully offline: no permissions, no network, no accounts, no tracking.

## [0.4.0] — 2026-07-04 (Phase 4 — production readiness)

### Added

- PDF export of checklists (day / range / month / year / entire history) using
  the platform `PdfDocument`, saved via the Storage Access Framework and
  shareable through a `FileProvider`.
- Offline backup & restore: a versioned, SHA-256-checksummed JSON snapshot of the
  whole database and preferences, restored atomically with validation.
- Expanded Settings: theme, date format, week start, animations toggle,
  include-notes-in-export, reset onboarding, open-source licences and a privacy
  statement.

## [0.3.0] — 2026-06-29 (Phase 3 — productivity)

### Added

- Calendar overview with per-day progress rings and deep-linking into a day.
- Searchable, filterable History timeline with expandable day cards.
- Statistics: completion metrics, weekly/monthly/trend charts (no chart
  libraries), per-task insights and achievement badges.
- Streak tracking (current and longest).

## [0.2.0] — 2026-06-28 (Phase 2 — the checklist engine)

### Added

- Checklist builder (add / edit / duplicate / delete / reorder tasks).
- Date-range generation of independent per-day checklists.
- Home screen: per-day pager, optimistic ticking, long-press edit/duplicate/
  delete with today-and-future scoping, add-task, and delete-with-undo.

### Changed

- Non-destructive Room migration v1 → v2 adding a nullable task `note` column.

## [0.1.0] — 2026-06-27 (Phase 1 — foundation)

### Added

- Project scaffold: Clean Architecture + MVVM, monochrome Material 3 design
  system, Room database, DataStore preferences, Hilt DI, Navigation Compose,
  first-run onboarding and the Home shell.

[1.0.0]: https://github.com/your-org/ticklog/releases/tag/v1.0.0
