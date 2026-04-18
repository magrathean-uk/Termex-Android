package com.termex.app.data.diagnostics

object DiagnosticsRedactor {
    private val pemBlockPattern = Regex(
        pattern = "-----BEGIN [^-]+-----.*?-----END [^-]+-----",
        options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val bearerPattern = Regex(
        pattern = "(?i)\\b(authorization\\s*:\\s*bearer)\\s+[^\\s]+"
    )
    private val assignmentPattern = Regex(
        pattern = "(?i)\\b(password|passphrase|secret|token|api[_-]?key)\\b\\s*([:=])\\s*([^\\s,;]+)"
    )
    private val privateKeyValuePattern = Regex(
        pattern = "(?i)\\b(private\\s*key)\\b\\s*([:=])\\s*([^\\s,;]+)"
    )
    private val uriUserInfoPattern = Regex(
        pattern = "(?i)\\b([a-z][a-z0-9+.-]*://)([^/@\\s]+)@"
    )

    fun redact(value: String?): String? {
        if (value.isNullOrBlank()) return value
        return value
            .replace(pemBlockPattern, "[REDACTED PEM BLOCK]")
            .replace(bearerPattern, "$1 [REDACTED]")
            .replace(assignmentPattern, "$1$2 [REDACTED]")
            .replace(privateKeyValuePattern, "$1$2 [REDACTED]")
            .replace(uriUserInfoPattern, "$1[REDACTED]@")
    }
}
