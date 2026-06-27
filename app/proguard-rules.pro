# =============================================================================
# ProGuard / R8 rules.
# -----------------------------------------------------------------------------
# Code shrinking is currently disabled for the release build (see build.gradle.kts).
# These rules are kept ready so enabling R8 later is a one-line change.
#
# Most libraries used here (Room, Hilt, Compose, Navigation) ship their own
# "consumer" keep rules, so this file only needs project-specific additions.
# =============================================================================

# Keep generic signatures and annotations used for reflection-based tooling.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Room entities and DAOs are referenced via generated code; keep their members.
-keep class com.ticklog.data.database.** { *; }
