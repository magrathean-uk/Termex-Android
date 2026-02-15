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

# Keep Apache MINA SSHD
-keep class org.apache.sshd.** { *; }
-dontwarn org.apache.sshd.**

# Keep BouncyCastle (optimize - only keep what's needed)
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.crypto.** { *; }
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.**

# Keep EdDSA
-keep class net.i2p.crypto.eddsa.** { *; }
-dontwarn net.i2p.crypto.eddsa.**

# Suppress sun.security warnings
-dontwarn sun.security.x509.X509Key
-dontwarn sun.security.**

# Suppress javax.security.auth warnings (not available on Android)
-dontwarn javax.security.auth.login.CredentialException
-dontwarn javax.security.auth.login.FailedLoginException
-dontwarn javax.security.auth.**

# Suppress SLF4J logging warnings
-dontwarn org.slf4j.**

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

