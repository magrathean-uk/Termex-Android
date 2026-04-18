package com.termex.app

import androidx.test.platform.app.InstrumentationRegistry

fun loadLiveSshFixtureOrNull(): LiveSshTestFixture? {
    val args = InstrumentationRegistry.getArguments()
    val payload = args.getString("termexFixtureBase64") ?: return null
    return LiveSshFixtureContract.fromBase64(payload)
}
