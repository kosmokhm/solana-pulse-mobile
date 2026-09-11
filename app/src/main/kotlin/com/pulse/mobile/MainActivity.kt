// MainActivity.kt - Entry point with Jetpack Compose
package com.pulse.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocationHelper.init(this)
        setContent {
            MaterialTheme {
                val walletManager = WalletConnectionManager(this)
                MainScreen(connectWallet = { walletManager.connectWallet() })
            }
        }
    }
}
