package com.scanflow.qr.utils

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.utils.LocaleHelper
import org.junit.Test
import java.util.Locale

class LocaleHelperTest {

    @Test
    fun `supported languages contains English and Indonesian`() {
        val supported = LocaleHelper.SUPPORTED_LANGUAGES
        assertThat(supported).containsKey("en")
        assertThat(supported).containsKey("id")
        assertThat(supported["en"]).isEqualTo("English (US)")
        assertThat(supported["id"]).isEqualTo("Bahasa Indonesia")
    }

    @Test
    fun `getDisplayName returns correct names and fallback`() {
        assertThat(LocaleHelper.getDisplayName("id")).isEqualTo("Bahasa Indonesia")
        assertThat(LocaleHelper.getDisplayName("en")).isEqualTo("English (US)")
        assertThat(LocaleHelper.getDisplayName("unknown")).isEqualTo("English (US)")
    }

    @Test
    fun `target locale codes match Java Locale language codes`() {
        val idLocale = Locale("id")
        val enLocale = Locale("en")

        assertThat(idLocale.language).isEqualTo("id")
        assertThat(enLocale.language).isEqualTo("en")
    }
}
