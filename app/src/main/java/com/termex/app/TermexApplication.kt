package com.termex.app

import android.app.Application
import android.os.StrictMode
import com.termex.app.core.security.AppLockCoordinator
import dagger.hilt.android.HiltAndroidApp
import org.apache.sshd.common.util.io.PathUtils
import java.nio.file.Paths

@HiltAndroidApp
class TermexApplication : Application() {

    @javax.inject.Inject
    lateinit var appLockCoordinator: AppLockCoordinator

    override fun onCreate() {
        super.onCreate()

        // MINA SSHD tries to resolve ~/.ssh via a static initializer that crashes on Android
        // (no home directory). Set the resolver before any MINA class is loaded.
        PathUtils.setUserHomeFolderResolver { Paths.get(filesDir.absolutePath) }

        appLockCoordinator.attachProcessLifecycle()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
