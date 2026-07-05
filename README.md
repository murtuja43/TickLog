<div align="center">

# TickLog

**An offline-first daily checklist recorder for Android.**

Define a checklist once, generate it across a date range, tick tasks off each
day, keep that history forever, review your statistics, glance at a home-screen
widget, and export everything to PDF.

Strictly **black & white**. Fully **offline**. No accounts, no network, no
tracking.

</div>

---

TickLog is built to feel like a premium Google application: a minimal Material 3
design with large typography, generous spacing, rounded corners and fluid
animations — in both light and dark modes. Everything lives on your device.

## Features

- **Daily checklist** — one screen per day with a smooth pager; tick tasks off
  with instant, optimistic feedback.
- **Create once, track for a range** — a first-run flow builds your checklist and
  generates an independent copy for every day in the range you choose.
- **History integrity** — editing your checklist only ever affects today and
  future days. Past days are immutable; task titles are snapshotted so your
  history never rewrites itself.
- **Per-day editing** — add, rename, note, duplicate, delete and reorder tasks,
  with a clear "this day only" vs. "this and all future days" choice, plus
  delete-with-undo.
- **Calendar** — a month overview with per-day progress rings; tap a day to open
  it.
- **History** — a searchable, filterable timeline of every day, with expandable
  cards.
- **Statistics** — completion metrics, weekly/monthly/trend charts (drawn on
  `Canvas`, no chart libraries), per-task insights, streaks and achievement
  badges.
- **Home-screen widget** — a monochrome Jetpack Glance widget showing today's
  date, completion percentage, tasks and streak. It adapts across 2×2 / 4×2 / 4×4,
  toggles tasks without opening the app, and refreshes automatically.
- **PDF export** — a clean A4 report for a day, range, month, year or your entire
  history; save via the system file picker or share it.
- **Offline backup & restore** — a versioned, checksummed JSON snapshot of
  everything, restored atomically with validation.
- **Your preferences** — theme (system / light / dark), date format, week start,
  animations toggle and more.
- **Accessible** — TalkBack labels throughout, large-font and landscape support,
  and adaptive tablet layouts.

## Screenshots

> Screenshots are captured on a Pixel-class device in both light and dark mode.
> Add your images to `docs/screenshots/` and reference them here.

| Home | Calendar | Statistics | Widget |
| ---- | -------- | ---------- | ------ |
| _tbd_ | _tbd_ | _tbd_ | _tbd_ |

## Tech stack

| Concern          | Choice                                             |
| ---------------- | -------------------------------------------------- |
| Language         | Kotlin                                             |
| UI               | Jetpack Compose + Material 3                       |
| Widget           | Jetpack Glance (app widgets)                        |
| Architecture     | MVVM + Clean Architecture (data / domain / ui)     |
| DI               | Hilt                                               |
| Persistence      | Room (history) + DataStore Preferences (UI flags)  |
| Async            | Coroutines + StateFlow                             |
| Navigation       | Navigation Compose                                 |
| Serialization    | kotlinx.serialization (backup format)              |
| PDF              | Platform `PdfDocument` (no third-party libraries)  |
| Splash           | androidx.core:core-splashscreen                    |
| Build            | Gradle Kotlin DSL + version catalog                |
| Min / Target SDK | 28 (Android 9) / 35                                |

The app uses **AndroidX only**, declares **no permissions**, and never touches
the network — it is fully offline by design.

## Architecture

A single `:app` Gradle module, organised by Clean Architecture layers so it can
be split into modules later without churn:

```
com.ticklog
├── core
│   ├── designsystem        # Theme (B&W Material 3) + reusable components
│   ├── navigation          # Destinations + the single NavHost
│   └── pdf                 # PdfDocument report renderer
├── data
│   ├── backup              # Versioned, checksummed JSON snapshot
│   ├── database            # Room: entities, DAOs, converters, relations
│   ├── datastore           # Preferences DataStore source
│   ├── model               # Entity <-> domain mappers
│   └── repository          # Repository implementations
├── domain
│   ├── calculator          # Pure streak / statistics / insight calculators
│   ├── model               # Pure business models
│   ├── repository          # Repository interfaces (the abstractions)
│   └── usecase             # Single-purpose application actions
├── di                      # Hilt modules + qualifiers
├── ui
│   ├── feature_onboarding  # First-run range + checklist builder
│   ├── feature_home        # The daily surface (per-day pager)
│   ├── feature_calendar    # Month overview with progress rings
│   ├── feature_history     # Searchable, filterable timeline
│   ├── feature_statistics  # Metrics, charts, insights, achievements
│   ├── feature_settings    # Preferences + licences
│   ├── feature_pdf         # PDF export flow
│   ├── feature_backup      # Backup & restore flow
│   └── feature_widget      # Home-screen Glance widget
└── util                    # Date/time formatting helpers
```

**Dependency rule:** `ui` and `data` depend on `domain`; `domain` depends on
nothing. ViewModels expose immutable UI state via `StateFlow`; composables are
stateless and hoisted; no business logic lives in the UI.

### Data model

Normalised Room schema (`version = 2`, schema exported under `app/schemas/`):

- `ChecklistTemplate` — the reusable blueprint (supports multiple via `isActive`).
- `ChecklistItem` — a template's task definitions (soft-deleted via `isArchived`).
- `DailyChecklist` — one checklist per template per date (unique index).
- `DailyChecklistItem` — per-day tasks; titles are **snapshotted** at generation
  time so editing a template never rewrites past days.
- `CompletionHistory` — per-day completion ledger powering history & statistics.
- `Settings` — single-row relational config (active template, week start, …).

UI-level flags (theme, onboarding completion, chosen date range) live in
**DataStore**, deliberately separate from the relational data.

### The widget

The widget is a `GlanceAppWidget` that reads today's checklist and streak through
the same `ChecklistRepository` the app uses (resolved via a Hilt `EntryPoint`,
since Glance components are not constructed by Hilt). It is never polled:
`updatePeriodMillis` is `0`. Instead it refreshes on real events —

- **in-app changes** are observed while the app is foregrounded and pushed to the
  widget (a `WidgetSynchronizer` tied to the process lifecycle),
- **task toggles from the widget** update it directly, and
- **the midnight rollover** is caught by the widget receiver's `DATE_CHANGED`
  handler —

which keeps it fresh while staying battery-friendly.

## Building

Requirements: **JDK 17** and the Android SDK (platform 35, build-tools 35.0.0).

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Run the unit tests
./gradlew :app:testDebugUnitTest

# Static analysis (warnings are treated as errors)
./gradlew :app:lintDebug
```

`local.properties` (pointing `sdk.dir` at your SDK) is git-ignored and
machine-specific.

### Installing

Build and install the debug APK on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

To add the widget, long-press the home screen, choose **Widgets**, find
**TickLog**, and drop it where you like — then resize it to taste.

### Release build (signed AAB for Google Play)

The release build is **shrunk and obfuscated** (R8 + resource shrinking) and is
signed from a keystore you provide. **No keystore or password is stored in the
repository** — signing material is read from a git-ignored `keystore.properties`
(or environment variables in CI).

**1. Generate your own upload keystore** (once). Keep the `.jks` file and its
passwords safe and private — losing them means you can't update the app:

```bash
keytool -genkeypair -v \
  -keystore ticklog-upload.jks \
  -alias ticklog \
  -keyalg RSA -keysize 2048 -validity 10000
```

**2. Point the build at it.** Copy the template and fill in your values:

```bash
cp keystore.properties.example keystore.properties
# then edit keystore.properties:
#   storeFile=ticklog-upload.jks
#   storePassword=…
#   keyAlias=ticklog
#   keyPassword=…
```

`keystore.properties` and `*.jks`/`*.keystore` are git-ignored. In CI, set the
`TICKLOG_KEYSTORE_FILE`, `TICKLOG_KEYSTORE_PASSWORD`, `TICKLOG_KEY_ALIAS` and
`TICKLOG_KEY_PASSWORD` environment variables instead of the file.

**3. Build the signed artifacts:**

```bash
# App Bundle (.aab) — the format you upload to Play
./gradlew :app:bundleRelease
# -> app/build/outputs/bundle/release/app-release.aab

# Signed APK (handy for local install / sideloading)
./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

If `keystore.properties` (and no env vars) is present, the release artifact is
**signed** and ready to upload; if it's absent the build still succeeds but
produces an **unsigned** artifact (so contributors without the keystore can
still build). Enrol the app in **Play App Signing** and upload the `.aab`.

## Testing

Business logic is covered by fast JVM unit tests: pure date/validation logic and
the calculators are tested directly, and the repository is exercised against an
in-memory Room database with Robolectric. PDF rendering is verified on-device
(the platform `PdfDocument` cannot render under Robolectric).

## Contributing

Contributions are welcome. Please:

1. Keep the **offline-first, zero-permission** guarantee intact.
2. Match the existing architecture (domain stays pure; UI stays stateless) and
   the strictly black-and-white design language.
3. Ensure `./gradlew :app:lintDebug :app:testDebugUnitTest` passes — lint runs
   with `warningsAsErrors = true`.
4. Add tests for any new business logic.

## Roadmap

Planned for future releases:

- Optional local reminders / notifications.
- Multiple concurrent checklists.
- Additional widget styles and quick-add.
- Localisation (all user-facing strings already live in `strings.xml`).

## License

Released under the [MIT License](LICENSE).
