package com.termex.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `server repair routes to exact server settings route`() {
        assertEquals(
            "server_settings?serverId=server-123&prefillHost=&prefillPort=0&prefillUser=&prefillKeyPath=&prefillCertificatePath=&prefillJumpHost=&prefillForwardAgent=false&prefillIdentitiesOnly=false&prefillForwards=",
            Route.ServerSettings.createRoute(serverId = "server-123")
        )
    }

    @Test
    fun `server settings route encodes prefills exactly`() {
        assertEquals(
            "server_settings?serverId=&prefillHost=prod.example.com&prefillPort=2222&prefillUser=deploy&prefillKeyPath=%2Fhome%2Fme%2F.id_ed25519&prefillCertificatePath=%2Fhome%2Fme%2F.id_ed25519-cert.pub&prefillJumpHost=jump.example.com&prefillForwardAgent=true&prefillIdentitiesOnly=true&prefillForwards=L%20127.0.0.1%3A8080%20%E2%86%92%20db.internal%3A5432",
            Route.ServerSettings.createRoute(
                serverId = null,
                prefillHost = "prod.example.com",
                prefillPort = 2222,
                prefillUser = "deploy",
                prefillKeyPath = "/home/me/.id_ed25519",
                prefillCertificatePath = "/home/me/.id_ed25519-cert.pub",
                prefillJumpHost = "jump.example.com",
                prefillForwardAgent = true,
                prefillIdentitiesOnly = true,
                prefillForwards = "L 127.0.0.1:8080 → db.internal:5432"
            )
        )
    }

    @Test
    fun `repair routes stay stable`() {
        assertEquals("key_repair/{serverId}", Route.KeyRepair.route)
        assertEquals("certificate_repair/{serverId}", Route.CertificateRepair.route)
        assertEquals("key_repair/server-123", Route.KeyRepair.createRoute("server-123"))
        assertEquals("certificate_repair/server-123", Route.CertificateRepair.createRoute("server-123"))
    }
}
