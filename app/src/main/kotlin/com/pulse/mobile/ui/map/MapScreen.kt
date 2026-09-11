// Map UI with interactive canvas, hotspot pins, and location check-in status
package com.pulse.mobile.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.data.HotspotType
import com.pulse.mobile.data.LocationHelper
import com.pulse.mobile.data.RewardResult
import com.pulse.mobile.data.TokenManager
import kotlinx.coroutines.launch

data class CheckInHotspot(
    val id: String,
    val name: String,
    val distanceMeters: Int,
    val rewardSkr: Int,
    val relativeX: Float, // 0.1f to 0.9f relative map coordinates
    val relativeY: Float,
    val hotspotType: HotspotType = HotspotType.STANDARD
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationStatusMessage by remember { mutableStateOf("Ready to scan location") }
    var isLocating by remember { mutableStateOf(false) }
    var rewardDialogResult by remember { mutableStateOf<RewardResult?>(null) }

    val hotspots = remember {
        listOf(
            CheckInHotspot("1", "Solana Mobile Hub", 120, 50, 0.35f, 0.45f, HotspotType.SPONSORED),
            CheckInHotspot("2", "Genesis Block Cafe", 340, 25, 0.65f, 0.30f, HotspotType.STANDARD),
            CheckInHotspot("3", "Web3 Solana Park", 500, 40, 0.70f, 0.75f, HotspotType.STANDARD),
            CheckInHotspot("4", "Proof of History Zone", 850, 100, 0.20f, 0.70f, HotspotType.EPIC_EVENT)
        )
    }

    var selectedHotspot by remember { mutableStateOf(hotspots.first()) }

    // Pulse animation for location radar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                isLocating = true
                val res = LocationHelper.getCurrentLocation()
                res.fold(
                    onSuccess = { loc ->
                        currentLocation = loc
                        locationStatusMessage = "📍 Verified at ${"%.4f".format(loc.first)}, ${"%.4f".format(loc.second)}"
                    },
                    onFailure = { err ->
                        locationStatusMessage = "Error: ${err.message}"
                    }
                )
                isLocating = false
            }
        } else {
            locationStatusMessage = "Permission denied"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pulse Map & Check-In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = locationStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        if (!LocationHelper.hasLocationPermission(context)) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            scope.launch {
                                isLocating = true
                                val res = LocationHelper.getCurrentLocation()
                                res.fold(
                                    onSuccess = { loc ->
                                        currentLocation = loc
                                        locationStatusMessage = "📍 ${"%.4f".format(loc.first)}, ${"%.4f".format(loc.second)}"
                                    },
                                    onFailure = { err ->
                                        locationStatusMessage = "Error: ${err.message}"
                                    }
                                )
                                isLocating = false
                            }
                        }
                    },
                    enabled = !isLocating
                ) {
                    Text(if (isLocating) "Scanning..." else "Locate Me")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Radar Map View Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10141E))
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = constraints.maxWidth.toFloat()
                val height = constraints.maxHeight.toFloat()
                val centerOffset = Offset(width / 2, height / 2)

                // Background radar grid & hotspots canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Grid lines
                    val primaryColor = Color(0xFF14F195)
                    val purpleColor = Color(0xFF9945FF)

                    // Concentric circles
                    drawCircle(color = primaryColor.copy(alpha = 0.15f), radius = width * 0.2f, center = centerOffset, style = Stroke(2f))
                    drawCircle(color = primaryColor.copy(alpha = 0.10f), radius = width * 0.35f, center = centerOffset, style = Stroke(2f))

                    // Pulsing User Location Marker
                    drawCircle(color = primaryColor.copy(alpha = pulseAlpha), radius = pulseRadius, center = centerOffset)
                    drawCircle(color = primaryColor, radius = 10f, center = centerOffset)

                    // Draw lines to hotspots
                    hotspots.forEach { spot ->
                        val spotPos = Offset(width * spot.relativeX, height * spot.relativeY)
                        val isSelected = spot.id == selectedHotspot.id
                        drawLine(
                            color = if (isSelected) primaryColor else purpleColor.copy(alpha = 0.3f),
                            start = centerOffset,
                            end = spotPos,
                            strokeWidth = if (isSelected) 3f else 1f
                        )
                    }
                }

                // Interactive Hotspot Pins overlay
                hotspots.forEach { spot ->
                    val spotX = (constraints.maxWidth * spot.relativeX).dp / LocalContext.current.resources.displayMetrics.density
                    val spotY = (constraints.maxHeight * spot.relativeY).dp / LocalContext.current.resources.displayMetrics.density

                    Box(
                        modifier = Modifier
                            .offset(x = spotX - 20.dp, y = spotY - 20.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (spot.id == selectedHotspot.id) Color(0xFF14F195) else Color(0xFF9945FF)
                            )
                            .clickable { selectedHotspot = spot },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${spot.rewardSkr}",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hotspots Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(hotspots) { spot ->
                FilterChip(
                    selected = spot.id == selectedHotspot.id,
                    onClick = { selectedHotspot = spot },
                    label = { Text("${spot.name} (${spot.distanceMeters}m)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Hotspot Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = selectedHotspot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Distance: ~${selectedHotspot.distanceMeters} meters away",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (!LocationHelper.hasLocationPermission(context)) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            scope.launch {
                                isLocating = true
                                val res = LocationHelper.getCurrentLocation()
                                res.fold(
                                    onSuccess = { loc ->
                                        currentLocation = loc
                                        val reward = TokenManager.processCheckIn(selectedHotspot.rewardSkr, selectedHotspot.hotspotType)
                                        rewardDialogResult = reward
                                        locationStatusMessage = "Checked in at ${selectedHotspot.name}!"
                                    },
                                    onFailure = { err ->
                                        locationStatusMessage = "Check-in failed: ${err.message}"
                                    }
                                )
                                isLocating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check In Here (Earn ~${selectedHotspot.rewardSkr} \$SKR)")
                }
            }
        }

        rewardDialogResult?.let { reward ->
            AlertDialog(
                onDismissRequest = { rewardDialogResult = null },
                confirmButton = {
                    Button(onClick = { rewardDialogResult = null }) {
                        Text("Awesome!")
                    }
                },
                title = { Text("🎉 Hotspot Check-In Successful!") },
                text = {
                    Column {
                        Text(
                            text = "+${reward.finalReward} \$SKR",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14F195)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Base Reward: ${reward.baseReward} \$SKR")
                        Text("Tier Multiplier: ${reward.tierMultiplier}x (${TokenManager.currentTier.displayName})")
                        Text("Hotspot Type: ${reward.hotspotMultiplier}x (${selectedHotspot.hotspotType.displayName})")
                        if (reward.isTierUpgraded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🏆 LEVEL UP! You unlocked ${reward.newTier.displayName}!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9945FF)
                            )
                        }
                    }
                }
            )
        }
    }
}


