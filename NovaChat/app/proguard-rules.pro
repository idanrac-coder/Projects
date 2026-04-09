# Strip verbose/debug/info/warning logs in release (keep only error)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Billing
-keep class com.android.vending.billing.**

# Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }

# LiteRT / TensorFlow Lite — keep Interpreter and JNI-loaded classes
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# SQLCipher – encrypted financial database
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**
