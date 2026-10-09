// MainActivity.kt - Entry point with Jetpack Compose
package com.pulse.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.mobile.ui.MainScreen
import com.pulse.mobile.data.LocationHelper
import com.pulse.mobile.mwa.WalletConnectionManager
import com.pulse.mobile.mwa.WalletSessionViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var walletManager: WalletConnectionManager
    private lateinit var walletSession: WalletSessionViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        LocationHelper.init(this)
        com.pulse.mobile.data.TokenManager.init(this)
        walletSession = ViewModelProvider(this, WalletSessionViewModel.factory(this))[WalletSessionViewModel::class.java]
        walletManager = WalletConnectionManager(this)
        walletSession.attach(walletManager)
        setContent {
            com.pulse.mobile.ui.PulseTheme {
                MainScreen(walletState = walletSession.state, connectWallet = walletSession::connect,
                    disconnectWallet = walletSession::disconnect, acceptWalletChange = walletSession::acceptChange,
                    rejectWalletChange = walletSession::rejectChange)
            }
        }
    }
    override fun onDestroy() {
        // Detach/cancel Activity-bound work only. Never revoke or clear a session here.
        if (::walletManager.isInitialized) walletSession.detach(walletManager)
        super.onDestroy()
    }
}
