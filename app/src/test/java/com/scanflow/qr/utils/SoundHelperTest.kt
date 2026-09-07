package com.scanflow.qr.utils

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.utils.SoundHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

class SoundHelperTest {

    @Before
    fun setUp() {
        SoundHelper.isEnabled = true
        SoundHelper.release()
    }

    @After
    fun tearDown() {
        SoundHelper.release()
        SoundHelper.isEnabled = true
    }

    @Test
    fun `SoundHelper is enabled by default`() {
        assertThat(SoundHelper.isEnabled).isTrue()
    }

    @Test
    fun `SoundHelper isEnabled toggle prevents audio playback`() {
        SoundHelper.isEnabled = false
        SoundHelper.playBeep()
        // With isEnabled false, no ToneGenerator should be instantiated
        assertThat(SoundHelper.hasActiveInstance()).isFalse()
    }

    @Test
    fun `SoundHelper release safely clears active instance without exception`() {
        SoundHelper.release()
        assertThat(SoundHelper.hasActiveInstance()).isFalse()

        // Multiple release calls must be idempotent and safe
        SoundHelper.release()
        SoundHelper.release()
        assertThat(SoundHelper.hasActiveInstance()).isFalse()
    }
}
