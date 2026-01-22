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

# Keep SSHJ
-keep class net.schmizz.sshj.** { *; }
-keep class com.hierynomus.** { *; }
-dontwarn net.schmizz.sshj.**
-dontwarn com.hierynomus.**

# Keep BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep EdDSA
-keep class net.i2p.crypto.eddsa.** { *; }
-dontwarn net.i2p.crypto.eddsa.**

# Suppress sun.security warnings
-dontwarn sun.security.x509.X509Key
-dontwarn sun.security.**

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
