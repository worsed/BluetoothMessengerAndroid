# Add project specific ProGuard rules here.
-keep class com.btmessenger.android.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
