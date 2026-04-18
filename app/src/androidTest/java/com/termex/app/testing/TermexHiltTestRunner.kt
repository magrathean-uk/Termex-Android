package com.termex.app.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import org.apache.sshd.common.util.io.PathUtils
import java.nio.file.Paths

class TermexHiltTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: android.os.Bundle?) {
        PathUtils.setUserHomeFolderResolver { Paths.get(targetContext.filesDir.absolutePath) }
        super.onCreate(arguments)
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(
            cl,
            HiltTestApplication::class.java.name,
            context
        )
    }
}
