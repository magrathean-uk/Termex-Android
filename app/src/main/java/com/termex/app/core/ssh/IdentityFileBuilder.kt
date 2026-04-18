package com.termex.app.core.ssh

import java.io.File

object IdentityFileBuilder {

    data class IdentityFiles(
        val privateKeyFile: File,
        val certificateFile: File
    )

    fun buildIdentityFiles(
        parentDir: File,
        serverId: String,
        privateKeyPath: String,
        certificatePath: String
    ): IdentityFiles {
        require(serverId.isNotBlank()) { "serverId required" }

        val safeServerId = serverId.replace(Regex("[^A-Za-z0-9._-]"), "_")

        parentDir.mkdirs()

        val privateKeyFile = File(parentDir, "termex-identity-$safeServerId")
        val certificateFile = File(parentDir, "termex-identity-$safeServerId-cert.pub")

        File(privateKeyPath).copyTo(privateKeyFile, overwrite = true)
        File(certificatePath).copyTo(certificateFile, overwrite = true)

        return IdentityFiles(
            privateKeyFile = privateKeyFile,
            certificateFile = certificateFile
        )
    }
}
