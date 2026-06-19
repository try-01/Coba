# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.samsungremote.**$$serializer { *; }
-keepclassmembers class com.samsungremote.** {
    *** Companion;
}
-keepclasseswithmembers class com.samsungremote.** {
    kotlinx.serialization.KSerializer serializer(...);
}
