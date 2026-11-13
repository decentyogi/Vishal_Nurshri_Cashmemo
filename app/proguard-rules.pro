# Keep generic signatures and annotations for Retrofit & Gson
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# Keep our DTOs and Domain Models used by Gson / Retrofit
-keep class com.vishalnursery.cashmemo.data.remote.dto.** { *; }
-keep class com.vishalnursery.cashmemo.data.ocr.** { *; }
-keep class com.vishalnursery.cashmemo.domain.model.** { *; }

# Retrofit
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.Platform$Java8
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
