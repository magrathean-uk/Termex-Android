# Termex ProGuard Rules

# Keep Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep JSch
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Keep Billing
-keep class com.android.billingclient.** { *; }

# Keep data classes
-keep class com.termex.app.domain.** { *; }
-keep class com.termex.app.data.local.*Entity { *; }

# Keep sealed classes
-keep class com.termex.app.core.ssh.SSHConnectionState { *; }
-keep class com.termex.app.core.ssh.SSHConnectionState$* { *; }
-keep class com.termex.app.core.billing.SubscriptionState { *; }
-keep class com.termex.app.core.billing.SubscriptionState$* { *; }
