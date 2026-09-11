// CheckInScreen.kt – location check-in UI with TokenManager integration
package com.pulse.mobile.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.data.HotspotType
import com.pulse.mobile.data.LocationHelper
import com.pulse.mobile.data.RewardResult
import com.pulse.mobile.data.TokenManager
import kotlinx.coroutines.launch

@Composable
fun CheckInScreen() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var rewardDialogResult by remember { mutableStateOf<RewardResult?>(null) }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!permissionGranted) {
            locationMessage = "Location permission denied"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            if (!permissionGranted && !LocationHelper.hasLocationPermission(context)) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                scope.launch {
                    val result = LocationHelper.getCurrentLocation()
                    result.fold(
                        onSuccess = { (lat, lng) ->
                            val reward = TokenManager.processCheckIn(50, HotspotType.SPONSORED)
                            rewardDialogResult = reward
                            locationMessage = "📍 Verified at $lat, $lng"
                        },
                        onFailure = { err ->
                            locationMessage = "Location error: ${err.message}"
                        }
                    )
                }
            }
        }) {
            Text("Check-In (Earn \$SKR)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        locationMessage?.let { Text(it) }

        rewardDialogResult?.let { reward ->
            AlertDialog(
                onDismissRequest = { rewardDialogResult = null },
                confirmButton = {
                    Button(onClick = { rewardDialogResult = null }) {
                        Text("Awesome!")
                    }
                },
                title = { Text("🎉 Check-In Successful!") },
                text = {
                    Column {
                        Text(
                            text = "+${reward.finalReward} \$SKR",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Base Reward: ${reward.baseReward} \$SKR")
                        Text("Tier Boost: ${reward.tierMultiplier}x (${TokenManager.currentTier.displayName})")
                        Text("Hotspot Boost: ${reward.hotspotMultiplier}x (Sponsored)")
                        if (reward.isTierUpgraded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🏆 LEVEL UP! You reached ${reward.newTier.displayName}!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            )
        }
    }
}

