# RuqaiyaPro Proguard Rules - Infinix HOT 12 Play
-keep class com.ruqaiyapro.debug.** { *; }
-keepclassmembers class com.ruqaiyapro.debug.** { *; }

# Keep Room DB entities & DAOs
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep OkHttp & JSON parsing
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**

# Compose runtime rules
-keep class androidx.compose.runtime.** { *; }

# WorkManager keep
-keep class androidx.work.** { *; }
