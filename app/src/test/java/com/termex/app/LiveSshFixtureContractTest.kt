package com.termex.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class LiveSshFixtureContractTest {

    @Test
    fun `decode live ssh fixture contract from base64`() {
        val payload = """
            {
              "vmName": "termex-vm",
              "distro": "ubuntu-24.04",
              "targetHostMode": "adb_reverse_loopback",
              "liveKey": {
                "host": "127.0.0.1",
                "port": 2222,
                "username": "demo",
                "serverName": "Live Key",
                "keyName": "id_ed25519",
                "keyText": "PRIVATE KEY"
              },
              "password": {
                "host": "127.0.0.1",
                "port": 2222,
                "username": "demo",
                "password": "secret",
                "serverName": "Password"
              },
              "certificate": {
                "host": "127.0.0.1",
                "port": 2222,
                "username": "demo",
                "serverName": "Certificate",
                "keyName": "id_ed25519",
                "keyText": "PRIVATE KEY",
                "certificateName": "id_ed25519-cert.pub",
                "certificateText": "CERT"
              },
              "jump": {
                "host": "127.0.0.1",
                "port": 2222,
                "username": "demo",
                "serverName": "Jump",
                "keyName": "id_jump",
                "targetHost": "127.0.0.1",
                "targetPort": 2200
              }
            }
        """.trimIndent()

        val fixture = LiveSshFixtureContract.fromBase64(
            Base64.getEncoder().encodeToString(payload.toByteArray(Charsets.UTF_8))
        )

        assertEquals("termex-vm", fixture.vmName)
        assertEquals("ubuntu-24.04", fixture.distro)
        assertEquals(TargetHostMode.ADB_REVERSE_LOOPBACK, fixture.targetHostMode)
        assertEquals("demo@127.0.0.1:2222", fixture.liveKey.connectionLabel)
        assertEquals("secret", fixture.password.password)
        assertEquals("CERT", fixture.certificate?.certificateText)
        assertEquals(2200, fixture.jump?.targetPort)
    }
}
