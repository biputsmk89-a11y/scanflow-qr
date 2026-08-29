package com.scanflow.qr

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.security.UrlSecurityChecker
import com.scanflow.qr.domain.model.SecurityLevel
import org.junit.Test

class UrlSecurityCheckerTest {

    @Test
    fun `standard https url is evaluated as safe`() {
        val result = UrlSecurityChecker.assessUrl("https://github.com/google")
        assertThat(result.isSecure).isTrue()
        assertThat(result.securityLevel).isEqualTo(SecurityLevel.SAFE)
        assertThat(result.isHttps).isTrue()
    }

    @Test
    fun `http url is flagged with warning`() {
        val result = UrlSecurityChecker.assessUrl("http://example.com")
        assertThat(result.isSecure).isFalse()
        assertThat(result.securityLevel).isEqualTo(SecurityLevel.WARNING)
        assertThat(result.isHttps).isFalse()
    }

    @Test
    fun `direct IP address host is flagged with warning`() {
        val result = UrlSecurityChecker.assessUrl("http://192.168.1.100/admin")
        assertThat(result.isSecure).isFalse()
        assertThat(result.securityLevel).isEqualTo(SecurityLevel.WARNING)
    }

    @Test
    fun `dangerous javascript scheme is blocked as dangerous`() {
        val result = UrlSecurityChecker.assessUrl("javascript:alert('xss')")
        assertThat(result.isSecure).isFalse()
        assertThat(result.securityLevel).isEqualTo(SecurityLevel.DANGEROUS)
    }

    @Test
    fun `suspicious tld is flagged with warning`() {
        val result = UrlSecurityChecker.assessUrl("https://free-gifts.xyz/claim")
        assertThat(result.isSecure).isFalse()
        assertThat(result.securityLevel).isEqualTo(SecurityLevel.WARNING)
    }
}
