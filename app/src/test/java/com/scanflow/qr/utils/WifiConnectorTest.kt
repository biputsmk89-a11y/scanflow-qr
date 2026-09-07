package com.scanflow.qr.utils

import android.content.ContextWrapper
import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.utils.WifiConnectionStatus
import com.scanflow.qr.core.utils.WifiConnector
import org.junit.Test

class WifiConnectorTest {

    @Test
    fun `blank ssid triggers FAILED status immediately`() {
        var returnedStatus: WifiConnectionStatus? = null
        var returnedMessage: String? = null

        val dummyContext = ContextWrapper(null)
        WifiConnector.connectToWifi(
            context = dummyContext,
            ssid = "",
            password = "123",
            securityType = "WPA"
        ) { status, msg ->
            returnedStatus = status
            returnedMessage = msg
        }

        assertThat(returnedStatus).isEqualTo(WifiConnectionStatus.FAILED)
        assertThat(returnedMessage).contains("tidak valid")
    }

    @Test
    fun `cancelCurrentConnection runs safely without active callback`() {
        // Calling cancel when nothing is active must not crash
        WifiConnector.cancelCurrentConnection()
    }

    @Test
    fun `WifiConnectionStatus enum contains all expected lifecycle states`() {
        val states = WifiConnectionStatus.entries
        assertThat(states).containsExactly(
            WifiConnectionStatus.IDLE,
            WifiConnectionStatus.CONNECTING,
            WifiConnectionStatus.CONNECTED,
            WifiConnectionStatus.FAILED
        )
    }
}
