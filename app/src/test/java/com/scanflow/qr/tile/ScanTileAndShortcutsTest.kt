package com.scanflow.qr.tile

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.navigation.Screen
import com.scanflow.qr.core.tile.ScanTileService
import org.junit.Test

class ScanTileAndShortcutsTest {

    @Test
    fun `tile service intent actions match standard shortcut intent definitions`() {
        assertThat(ScanTileService.ACTION_SCAN).isEqualTo("com.scanflow.qr.action.SCAN")
        assertThat(ScanTileService.ACTION_CREATE).isEqualTo("com.scanflow.qr.action.CREATE")
        assertThat(ScanTileService.ACTION_HISTORY).isEqualTo("com.scanflow.qr.action.HISTORY")
    }

    @Test
    fun `shortcut actions map directly to correct navigation routes`() {
        val actionMap = mapOf(
            ScanTileService.ACTION_SCAN to Screen.Scanner.route,
            ScanTileService.ACTION_CREATE to Screen.CreateQr.route,
            ScanTileService.ACTION_HISTORY to Screen.History.route
        )

        assertThat(actionMap[ScanTileService.ACTION_SCAN]).isEqualTo("scanner")
        assertThat(actionMap[ScanTileService.ACTION_CREATE]).isEqualTo("create_qr")
        assertThat(actionMap[ScanTileService.ACTION_HISTORY]).isEqualTo("history")
    }
}
