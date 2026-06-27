# TickLog

An **offline-first daily checklist recorder** for Android. TickLog lets you define a
checklist once, generate it across a date range, tick tasks off each day, keep that
history forever, review your statistics, and export everything to PDF.

It is built to feel like a premium Google application: a strictly **black & white**,
minimal Material 3 design with large typography, generous spacing, rounded corners and
fluid animations — in both light and dark modes.

> **Status: Phase 1 (foundation).** This phase delivers the full architecture, design
> system, database, navigation, onboarding and the Home shell. Checklist creation and
> completion arrive in **Phase 2**.

---

## Tech stack

| Concern              | Choice                                                        |
| -------------------- | ------------------------------------------------------------- |
| Language             | Kotlin                                                        |
| UI                   | Jetpack Compose + Material 3                                  |
| Architecture         | MVVM + Clean Architecture (data / domain / ui)               |
| DI                   | Hilt                                                          |
| Persistence          | Room (history) + DataStore Preferences (UI flags)            |
| Async                | Coroutines + StateFlow                                        |
| Navigation           | Navigation Compose                                            |
| Build                | Gradle Kotlin DSL + Version Catalog                           |
| Min / Target SDK     | 28 (Android 9) / 35                                           |

The app uses **AndroidX only**, declares **no permissions**, and never touches the
network — it is fully offline by design.

## Architecture

A single `:app` Gradle module, organised by Clean Architecture layers so it can be split
into modules later without churn:

```
com.ticklog
├── core
│   ├── designsystem        # Theme (B&W Material 3) + reusable components
│   └── navigation          # Destinations + the single NavHost
├── data
│   ├── database            # Room: entities, DAOs, converters, relations
│   ├── datastore           # Preferences DataStore source
│   ├── model               # Entity <-> domain mappers
│   └── repository          # Repository implementations
├── domain
│   ├── model               # Pure business models
│   ├── repository          # Repository interfaces (the abstractions)
│   └── usecase             # Single-purpose application actions
├── di                      # Hilt modules + qualifiers
├── ui
│   ├── feature_onboarding  # First-run date-range selection
│   ├── feature_home        # Daily surface (day-swiping prepared)
│   ├── feature_calendar    # (Phase 2)
│   ├── feature_history     # (Phase 2)
│   ├── feature_statistics  # (Phase 2)
│   ├── feature_settings    # Theme selection
│   ├── feature_pdf         # (Phase 2)
│   └── feature_widget      # Reserved for a Glance widget (Phase 2)
└── util                    # Date/time formatting helpers
```

**Dependency rule:** `ui` and `data` depend on `domain`; `domain` depends on nothing.
ViewModels expose immutable UI state via `StateFlow`; composables are stateless and
hoisted; no business logic lives in the UI.

### Data model

Normalised Room schema (`version = 1`, schema exported under `app/schemas/`):

- `ChecklistTemplate` — the reusable blueprint (supports multiple via `isActive`).
- `ChecklistItem` — a template's task definitions (soft-deleted via `isArchived`).
- `DailyChecklist` — one checklist per template per date (unique index).
- `DailyChecklistItem` — per-day tasks; titles are **snapshotted** at generation time so
  editing a template never rewrites past days (the "preserve history forever" guarantee).
- `CompletionHistory` — per-day completion ledger powering history & statistics.
- `Settings` — single-row relational config (active template, reminders, week start).

UI-level flags (theme, onboarding completion, chosen date range) live in **DataStore**,
deliberately separate from the relational data so neither store duplicates the other.

## Building

Requirements: **JDK 17** and the Android SDK (platform 35, build-tools 35.0.0).

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Static analysis
./gradlew :app:lintDebug
```

`local.properties` (pointing `sdk.dir` at your SDK) is git-ignored and machine-specific.

## License

Proprietary — all rights reserved (placeholder; update as appropriate).
