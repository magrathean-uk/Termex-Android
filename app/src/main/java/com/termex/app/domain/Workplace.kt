package com.termex.app.domain

import java.util.UUID

data class Workplace(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)
