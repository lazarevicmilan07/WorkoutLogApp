# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.workoutlog.**$$serializer { *; }
-keepclassmembers class com.workoutlog.** { *** Companion; }
-keepclasseswithmembers class com.workoutlog.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Apache POI
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }

# iText
-dontwarn com.itextpdf.**
-keep class com.itextpdf.** { *; }
