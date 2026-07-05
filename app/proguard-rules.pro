# =============================================================================
# ProGuard / R8 rules for the release build (isMinifyEnabled = true).
# -----------------------------------------------------------------------------
# Most libraries (Room, Hilt, Compose, Navigation, Glance, kotlinx.serialization)
# ship their own R8 "consumer" rules, so this file only adds the project-specific
# keeps R8 cannot infer on its own. Manifest-declared components (Activity,
# Application, the widget receiver, the FileProvider) are kept automatically.
# =============================================================================

# Keep generic signatures and the annotations reflection-based tooling reads.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# --- Room --------------------------------------------------------------------
# Entities and DAOs are wired through generated code and (de)serialised by name.
-keep class com.ticklog.data.database.** { *; }

# --- kotlinx.serialization (offline backup format) ---------------------------
# The compiler plugin generates a synthetic serializer for every @Serializable
# type; these rules keep the generated `Companion`/`serializer()` entry points so
# backup export and restore keep working after shrinking. (The library also ships
# consumer rules; these make the guarantee explicit for our own DTOs.)
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the generated $$serializer classes for our backup DTOs outright.
-keep, includedescriptorclasses class com.ticklog.data.backup.**$$serializer { *; }
-keepclassmembers class com.ticklog.data.backup.** { *; }

# --- Glance widget -----------------------------------------------------------
# ActionCallbacks (e.g. the task-toggle) are instantiated reflectively by the
# Glance framework, so keep their no-arg constructors.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>(...);
}
