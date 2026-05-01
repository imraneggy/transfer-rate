# R8 / ProGuard rules.
#
# Defaults from `proguard-android-optimize.txt` plus rules required by our
# specific dependencies. R8 full-mode (enabled in gradle.properties) is more
# aggressive than ProGuard, so we keep this file lean.

# kotlinx.serialization — keep generated $serializer companions and the
# associated metadata. Without these rules, JSON deserialisation crashes
# in release with NoSuchFieldError.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static kotlinx.serialization.KSerializer serializer(...);
}

# Strip android.util.Log statements at debug level to avoid leaking
# diagnostic info into a shipped APK.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

# Don't print verbose logs about the rules engine.
-dontnote
