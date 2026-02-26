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

# Keep Room type converters
-keep class com.termex.app.data.local.Converters { *; }

# Keep Apache MINA SSHD
-keep class org.apache.sshd.** { *; }
-dontwarn org.apache.sshd.**

# Keep BouncyCastle
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.crypto.** { *; }
-keep class org.bouncycastle.openssl.** { *; }
-keep class org.bouncycastle.pkcs.** { *; }
-keep class org.bouncycastle.asn1.** { *; }
-keep class org.bouncycastle.operator.** { *; }
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.**

# Keep EdDSA
-keep class net.i2p.crypto.eddsa.** { *; }
-dontwarn net.i2p.crypto.eddsa.**

# Keep SLF4J
-keep class org.slf4j.** { *; }
-keep class org.slf4j.impl.** { *; }
-dontwarn org.slf4j.**

# Suppress sun.security warnings
-dontwarn sun.security.x509.X509Key
-dontwarn sun.security.**

# Suppress javax.security.auth warnings (not available on Android)
-dontwarn javax.security.auth.login.CredentialException
-dontwarn javax.security.auth.login.FailedLoginException
-dontwarn javax.security.auth.**

# Keep Billing
-keep class com.android.billingclient.** { *; }

# Keep data classes and domain models
-keep class com.termex.app.domain.** { *; }
-keep class com.termex.app.data.local.*Entity { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep sealed classes
-keep class com.termex.app.core.ssh.SSHConnectionState { *; }
-keep class com.termex.app.core.ssh.SSHConnectionState$* { *; }
-keep class com.termex.app.core.billing.SubscriptionState { *; }
-keep class com.termex.app.core.billing.SubscriptionState$* { *; }

