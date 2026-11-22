# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.recipeapp.syntages.** {
    *** Companion;
}
-keepclasseswithmembers class com.recipeapp.syntages.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.recipeapp.syntages.**$$serializer { *; }
