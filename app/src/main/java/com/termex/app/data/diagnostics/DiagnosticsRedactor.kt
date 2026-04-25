package com.termex.app.data.diagnostics

object DiagnosticsRedactor {
    private val pemBlockPattern = Regex(
        pattern = "-----BEGIN [^-]+-----.*?-----END [^-]+-----",
        options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    private val bearerPattern = Regex(
        pattern = "(?im)\\b(authorization\\s*:\\s*bearer)\\s+[^\\s]+"
    )
    private val cookieHeaderPattern = Regex(
        pattern = "(?im)^(\\s*(?:set-cookie|cookie)\\s*:)\\s*[^\\r\\n]+"
    )
    private val assignmentPattern = Regex(
        pattern = "(?im)(?<!-)\\b([A-Z0-9_.-]*(?:PASSWORD|PASSWD|PASSPHRASE|SECRET|TOKEN|API[_-]?KEY|PRIVATE[_-]?KEY|ACCESS[_-]?KEY)[A-Z0-9_.-]*)\\b\\s*([:=])\\s*(\"[^\"\\r\\n]*\"|'[^'\\r\\n]*'|[^\\s,;]+)",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    private val privateKeyValuePattern = Regex(
        pattern = "(?im)\\b(private\\s*key)\\b\\s*([:=])\\s*(\"[^\"\\r\\n]*\"|'[^'\\r\\n]*'|[^\\s,;]+)"
    )
    private val uriUserInfoPattern = Regex(
        pattern = "(?i)\\b([a-z][a-z0-9+.-]*://)([^/@\\s]+)@"
    )
    private val cliSecretPattern = Regex(
        pattern = "(?im)(\\B--?(?:password|passphrase|token|api-key|secret)(?:=|\\s+))(\"[^\"\\r\\n]*\"|'[^'\\r\\n]*'|[^\\s]+)"
    )
    private val sshpassPattern = Regex(
        pattern = "(?im)(\\bsshpass\\s+-p\\s+)(\"[^\"\\r\\n]*\"|'[^'\\r\\n]*'|[^\\s]+)"
    )
    private val sshConfigSensitiveLinePattern = Regex(
        pattern = "(?im)^(\\s*(?:IdentityFile|CertificateFile|IdentityAgent|ProxyCommand)\\s+).+$"
    )
    private val jsonSecretPattern = Regex(
        pattern = "(?im)(\"(?:password|passphrase|secret|token|api[_-]?key|private[_-]?key|access[_-]?key|authorization)\"\\s*:\\s*)(\"[^\"\\r\\n]*\"|[^,}\\r\\n]+)",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun redact(value: String?): String? {
        if (value.isNullOrBlank()) return value
        return value
            .replace(pemBlockPattern, "[REDACTED PEM BLOCK]")
            .replace(bearerPattern, "$1 [REDACTED]")
            .replace(cookieHeaderPattern, "$1 [REDACTED]")
            .replace(uriUserInfoPattern, "$1[REDACTED]@")
            .replace(sshpassPattern, "$1[REDACTED]")
            .replace(cliSecretPattern, "$1[REDACTED]")
            .replace(assignmentPattern, "$1$2 [REDACTED]")
            .replace(privateKeyValuePattern, "$1$2 [REDACTED]")
            .replace(sshConfigSensitiveLinePattern, "$1[REDACTED]")
            .replace(jsonSecretPattern, "$1\"[REDACTED]\"")
    }
}
