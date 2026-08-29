package com.scanflow.qr.core.security

import com.scanflow.qr.domain.model.SecurityAssessment
import com.scanflow.qr.domain.model.SecurityLevel
import java.net.URI

object UrlSecurityChecker {

    private val suspiciousTlds = setOf(
        ".zip", ".mov", ".top", ".xyz", ".work", ".click", ".link", ".gq", ".ml", ".cf", ".ga", ".tk"
    )

    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "buff.ly", "ow.ly", "cutt.ly", "rb.gy"
    )

    private val dangerousSchemes = setOf(
        "javascript", "data", "file", "intent"
    )

    fun assessUrl(url: String): SecurityAssessment {
        val trimmed = url.trim()
        val details = mutableListOf<String>()

        try {
            val schemeIndex = trimmed.indexOf(":")
            val scheme = if (schemeIndex > 0) trimmed.substring(0, schemeIndex).lowercase() else ""

            // Extract host safely without Android framework dependency
            val host = extractHost(trimmed, scheme)

            if (scheme in dangerousSchemes) {
                return SecurityAssessment(
                    isSecure = false,
                    securityLevel = SecurityLevel.DANGEROUS,
                    summary = "Dangerous URI Scheme ($scheme:)",
                    details = listOf("Execution of raw scripts or internal intents is blocked for security."),
                    isHttps = false,
                    domain = host
                )
            }

            var isHttps = false
            var isSuspicious = false

            if (scheme == "https") {
                isHttps = true
                details.add("Uses secure encrypted HTTPS protocol.")
            } else if (scheme == "http") {
                details.add("Unencrypted HTTP protocol. Data transmitted may be intercepted.")
                isSuspicious = true
            } else if (scheme.isNotEmpty()) {
                details.add("Custom or non-standard protocol ($scheme).")
            }

            if (!host.isNullOrEmpty()) {
                // Check if host is raw IP address (e.g. 192.168.1.1 or 45.33.32.156)
                val isIpAddress = host.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))
                if (isIpAddress) {
                    isSuspicious = true
                    details.add("Direct IP address host detected ($host). Often used in untrusted or malicious links.")
                }

                // Check suspicious TLD
                if (suspiciousTlds.any { host.endsWith(it) }) {
                    isSuspicious = true
                    details.add("Domain uses a top-level domain frequently associated with spam or phishing.")
                }

                // Check URL Shortener
                if (urlShorteners.contains(host)) {
                    details.add("Shortened URL service detected. Target destination is hidden until opened.")
                }

                // Check multiple subdomains / phishing spoofing
                val parts = host.split(".")
                if (parts.size > 4) {
                    isSuspicious = true
                    details.add("High number of subdomains detected. May be an attempt to impersonate legitimate services.")
                }
            }

            val level = when {
                scheme in dangerousSchemes -> SecurityLevel.DANGEROUS
                !isHttps -> SecurityLevel.WARNING
                isSuspicious -> SecurityLevel.WARNING
                else -> SecurityLevel.SAFE
            }

            val summary = when (level) {
                SecurityLevel.SAFE -> "Verified HTTPS Connection (Legitimate Structure)"
                SecurityLevel.WARNING -> "Caution: Potential Security / Privacy Risks"
                SecurityLevel.DANGEROUS -> "Blocked: High Risk Malicious Payload"
                SecurityLevel.UNKNOWN -> "Unknown Link Structure"
            }

            return SecurityAssessment(
                isSecure = level == SecurityLevel.SAFE,
                securityLevel = level,
                summary = summary,
                details = details,
                isHttps = isHttps,
                domain = host
            )
        } catch (e: Exception) {
            return SecurityAssessment(
                isSecure = false,
                securityLevel = SecurityLevel.UNKNOWN,
                summary = "Failed to parse link structure",
                details = listOf("Encountered parsing error: ${e.localizedMessage}"),
                isHttps = false,
                domain = null
            )
        }
    }

    private fun extractHost(url: String, scheme: String): String? {
        return try {
            val uri = URI(url)
            uri.host?.lowercase()
        } catch (e: Exception) {
            val afterScheme = if (scheme.isNotEmpty()) url.substringAfter("://").substringAfter(":") else url
            val cleanPath = afterScheme.substringBefore("/").substringBefore("?").substringBefore("#")
            cleanPath.substringBefore(":").lowercase().ifEmpty { null }
        }
    }
}
