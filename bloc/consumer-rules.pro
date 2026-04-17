# ── kotlinx.serialization ────────────────────────────────────────────────────
# Required by HydratedBloc, which uses kotlinx.serialization to persist state.

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializer companion objects
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated $serializer classes for any @Serializable state types
-keepclasseswithmembers class **$$serializer {
    *;
}

# ── kotlinx.coroutines ───────────────────────────────────────────────────────
# Coroutines use reflection internally for debug mode; keep relevant symbols.

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── BlocKotlin ───────────────────────────────────────────────────────────────
# Preserve Bloc/Cubit class names used in BlocObserver log messages and
# BlocRegistry error messages (both reference ::class.simpleName).

-keepnames class dev.bloc.** { *; }
