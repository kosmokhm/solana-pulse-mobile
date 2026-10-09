package com.pulse.mobile.ui.map

import android.Manifest
import android.content.Intent
import android.content.Context
import android.location.LocationManager
import android.location.Location
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.data.*
import com.pulse.mobile.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.*

private enum class DemoAction(val loading: String) {
    CREATE("Finding a precise GPS fix…"),
    REFRESH("Refreshing GPS…"),
    CHECK_IN("Verifying your check-in…")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(walletContent: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generation = remember { TokenManager.demoGeneration }
    val hotspot = TokenManager.demoHotspot
    val claimed = TokenManager.demoRewardClaimed
    val saveUncertain = TokenManager.demoRewardSaveUncertain
    var fix by remember { mutableStateOf<Location?>(null) }
    var pendingAction by remember { mutableStateOf<DemoAction?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var reward by remember { mutableStateOf<RewardResult?>(null) }
    var clockTick by remember { mutableStateOf(SystemClock.elapsedRealtimeNanos()) }
    var hasPrecisePermission by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
    fun gpsIsEnabled() = runCatching {
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
            .isProviderEnabled(LocationManager.GPS_PROVIDER)
    }.getOrDefault(false)
    var gpsEnabled by remember { mutableStateOf(gpsIsEnabled()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockTick = SystemClock.elapsedRealtimeNanos()
            // Reads settings only, never requests coordinates or changes permissions.
            hasPrecisePermission = LocationHelper.hasLocationPermission(context)
            gpsEnabled = gpsIsEnabled()
            delay(1_000)
        }
    }
    val busy = pendingAction != null || LocationHelper.isLocating
    val distance = hotspot?.let { spot -> fix?.let(spot::distanceMeters) }
    // Reading the tick makes freshness and range expire on screen, without background GPS.
    val stale = fix?.let { clockTick - it.elapsedRealtimeNanos !in 0..30_000_000_000L } ?: false
    val rejection = if (stale) "GPS fix expired. Refresh GPS." else hotspot?.let { spot ->
        fix?.let(spot::rejectionReason)
    }
    val inRange = hotspot != null && fix != null && rejection == null
    val rangeLabel = when {
        hotspot == null -> "No test zone yet"
        fix == null -> "Awaiting GPS"
        stale -> "Refresh GPS"
        inRange -> "In range"
        rejection?.startsWith("Outside geofence") == true -> "Out of range"
        else -> "GPS not ready"
    }
    val rangeColor = if (inRange) PulseMint else if (fix == null) PulseMuted else Color(0xFFFFCE86)

    fun scan(action: DemoAction) {
        if (generation != TokenManager.demoGeneration) return
        val target = TokenManager.demoHotspot
        scope.launch {
            try {
                val location = withTimeout(20_000) { LocationHelper.getCurrentLocation().getOrThrow() }
                ensureActive()
                check(generation == TokenManager.demoGeneration) { "Demo data was reset. Start a new request." }
                fix = location
                clockTick = SystemClock.elapsedRealtimeNanos()
                val qualityIssue = HotspotConfig.localHotspot(location.latitude, location.longitude)
                    .rejectionReason(location)
                if (qualityIssue != null) throw IllegalArgumentException(qualityIssue)
                when (action) {
                    DemoAction.CREATE -> {
                        TokenManager.createTestHotspot(location, generation)
                        message = "Test hotspot created. Your 100 m demo zone is ready."
                    }
                    DemoAction.REFRESH -> {
                        val issue = target?.rejectionReason(location)
                        isError = issue != null
                        message = issue ?: "GPS refreshed. You are inside the test zone."
                    }
                    DemoAction.CHECK_IN -> {
                        checkNotNull(target) { "Create a test hotspot first" }
                        reward = TokenManager.processCheckIn(target, location, generation)
                        message = "Check-in complete. +${reward!!.finalReward} local Pulse Points."
                    }
                }
            } catch (e: TimeoutCancellationException) {
                isError = true
                message = "GPS timed out after 20 seconds. Move outdoors and try again."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isError = true
                message = e.message ?: "Could not verify GPS. Please try again."
            } finally {
                pendingAction = null
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val action = pendingAction
        if (action != null) {
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) scan(action)
            else {
                pendingAction = null
                isError = true
                message = "Precise location permission is required. Enable it in app settings, then try again."
            }
        }
    }

    fun request(action: DemoAction) {
        // Read live state as two taps can arrive before Compose recomposes.
        if (generation != TokenManager.demoGeneration || pendingAction != null || LocationHelper.isLocating || saveUncertain) return
        if (action != DemoAction.REFRESH && TokenManager.demoRewardClaimed) return
        pendingAction = action
        fix = null
        message = null
        isError = false
        if (LocationHelper.hasLocationPermission(context)) scan(action)
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        walletContent()
        PulseBalanceCard(TokenManager.balancePoints, "", compact = true)
        Surface(shape = RoundedCornerShape(24.dp), color = PulseSurface,
            border = BorderStroke(1.dp, PulseOutline)) {
            Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF1D1836), PulseSurface)))
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulseIcon(PulseGlyph.Radar, color = PulseMint, modifier = Modifier.size(18.dp))
                        Text("Zone Radar", style = MaterialTheme.typography.labelSmall)
                    }
                    PulseBadge(rangeLabel, rangeColor)
                }
                RadarPanel(hotspot, fix, inRange)
                Text("Schematic radar, not a geographic map", modifier = Modifier.fillMaxWidth(), color = PulseMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp))
                Text("GPS: " + when {
                    busy -> "Acquiring precise location"
                    !hasPrecisePermission -> "Precise permission required"
                    !gpsEnabled -> "Off — enable device location"
                    fix == null -> "Not scanned — on demand"
                    stale -> "Fix expired — refresh required"
                    else -> "Fresh fix • $rangeLabel"
                }, modifier = Modifier.fillMaxWidth(), color = rangeColor, style = MaterialTheme.typography.bodySmall)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val accuracyText = fix?.takeIf { it.hasAccuracy() }?.accuracy?.let {
                        if (it.isFinite()) "±${it.roundToInt()} m" else "Unavailable"
                    } ?: "Not scanned"
                    val distanceText = distance?.takeIf { it.isFinite() }?.let { "${it.roundToInt()} m" } ?: "Not scanned"
                    if (maxWidth >= 280.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PulseStat("GPS accuracy", accuracyText, PulseGlyph.Radar, Modifier.weight(1f))
                            PulseStat("Distance to center", distanceText, PulseGlyph.Pin, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PulseStat("GPS accuracy", accuracyText, PulseGlyph.Radar, Modifier.fillMaxWidth())
                            PulseStat("Distance to center", distanceText, PulseGlyph.Pin, Modifier.fillMaxWidth())
                        }
                    }
                }
                if (fix != null) Text(if (stale) "Last fix expired. Refresh GPS to verify your position."
                    else "Last GPS scan. Check-in always requests a fresh fix.", modifier = Modifier.fillMaxWidth(), color = PulseMuted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        PulsePanel(accent = PulseMint) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).background(PulseMint.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center) { PulseIcon(PulseGlyph.Pin, color = PulseMint) }
                Column(Modifier.weight(1f)) {
                    Text("01 / YOUR LOCAL ZONE", color = PulseMint, style = MaterialTheme.typography.labelSmall)
                    Text("Demo Hotspot", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(when {
                saveUncertain -> "The reward save could not be confirmed. Close and restart the app to verify your balance."
                claimed -> "Mission completed. Receipt saved on this device."
                hotspot != null -> "Test zone ready. Check in with a fresh GPS fix."
                else -> "Demo Setup: create a 100 m zone at your GPS position."
            }, color = PulseMuted, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseBadge("100 M RADIUS", Color(0xFFC4A2FF))
                PulseBadge("GPS ≤ 30 M", PulseMint)
            }
            Text("Check-in: " + when {
                saveUncertain -> "Blocked — restart to verify save"
                claimed -> "Completed — repeat claim blocked"
                busy -> "GPS request in progress"
                hotspot == null -> "Create a test zone first"
                else -> "Available — fresh GPS verification required"
            }, color = if (claimed) PulseMint else PulseMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (claimed && !saveUncertain) {
                Text(TokenManager.lastCheckInPoints?.let { "Received +$it Pulse Points" }
                    ?: "Reward recorded earlier (amount not stored)", color = PulseMint,
                    style = MaterialTheme.typography.titleMedium)
                Text(TokenManager.lastCheckInAtMillis?.let {
                    "Successful check-in: ${formatReceiptTime(it)} (device time)"
                } ?: "Earlier receipt has no recorded check-in time", color = PulseMuted,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (hotspot == null && !claimed) {
                OutlinedButton(onClick = { request(DemoAction.CREATE) }, enabled = !busy && !saveUncertain,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PulseViolet)) {
                    PulseIcon(PulseGlyph.Pin, color = Color(0xFFC4A2FF))
                    Spacer(Modifier.width(10.dp))
                    Text("Create Test Hotspot Here", color = Color.White)
                }
            }
            if (hotspot != null) {
                if (!claimed) {
                Text("CHECK-IN REWARD", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
                Text("${TokenManager.calculateReward(hotspot.rewardPoints, TokenManager.currentTier, hotspot.hotspotType)} Pulse Points",
                    color = PulseMint, style = MaterialTheme.typography.headlineMedium)
                }
                PulseAction(if (saveUncertain) "Restart to verify save" else if (claimed) "Reward received" else "Check in to this zone",
                    onClick = { request(DemoAction.CHECK_IN) }, enabled = !busy && !claimed && !saveUncertain,
                    loading = pendingAction == DemoAction.CHECK_IN,
                    glyph = if (claimed) PulseGlyph.Check else PulseGlyph.Arrow)
                OutlinedButton(onClick = { request(DemoAction.REFRESH) }, enabled = !busy && !saveUncertain,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                    PulseIcon(PulseGlyph.Refresh, color = PulseMuted)
                    Spacer(Modifier.width(10.dp))
                    Text("Refresh GPS & Distance", color = Color.White)
                }
                TextButton(onClick = {
                    try {
                        TokenManager.removeTestHotspot()
                        fix = null
                        isError = false
                        message = "Local location removed. Any reward receipt is kept."
                    } catch (e: Exception) { isError = true; message = e.message }
                }, enabled = !busy && !saveUncertain, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Remove local hotspot location", color = PulseMuted)
                }
            }
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = PulseMint)
                Text(pendingAction?.loading ?: "GPS request in progress…", color = PulseMint,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            message?.let { PulseNotice(it, isError) }
            if (isError) {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")))
                }, enabled = !busy) { Text("App permission settings") }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }, enabled = !busy) { Text("Device location settings") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            PulseIcon(PulseGlyph.Shield, color = PulseMuted, modifier = Modifier.size(20.dp))
            Text("Local demo points. GPS center stays on-device. Local checks are not server anti-fraud.",
                color = PulseMuted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
    }
    reward?.let { result ->
        AlertDialog(onDismissRequest = { reward = null },
            icon = { PulseIcon(PulseGlyph.Check, color = PulseMint, modifier = Modifier.size(40.dp)) },
            title = { Text("That's your pulse.") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("+${result.finalReward}", color = PulseMint, style = MaterialTheme.typography.displayLarge)
                    PulseBadge("PULSE POINTS / LOCAL DEMO")
                    Text("GPS and the 100 m zone were verified. Your receipt is saved on this device.", color = PulseMuted)
                    if (result.isTierUpgraded) Text("New tier: ${result.newTier.displayName}", color = Color(0xFFC4A2FF))
                }
            }, confirmButton = { TextButton(onClick = { reward = null }) { Text("Keep exploring") } })
    }
}

@Composable
private fun RadarPanel(hotspot: DemoHotspot?, fix: Location?, inRange: Boolean) {
    val bearing = if (hotspot != null && fix != null) FloatArray(2).also {
        Location.distanceBetween(hotspot.latitude, hotspot.longitude, fix.latitude, fix.longitude, it)
    } else null
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val panelHeight = maxWidth.coerceAtMost(420.dp)
        val compassPadding = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 4.dp
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.fillMaxWidth().height(panelHeight), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(compassPadding).clearAndSetSemantics { }) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = min(size.width, size.height) * 0.33f
                    drawCircle(Brush.radialGradient(listOf(PulseViolet.copy(alpha = 0.22f), Color.Transparent),
                        center, radius * 1.45f), radius * 1.45f, center)
                    // Decorative instrument ticks, not geographic pins or activity data.
                    repeat(72) { tick ->
                        val angle = Math.toRadians(tick * 5.0)
                        val axis = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
                        drawLine(if (tick % 6 == 0) PulseMint.copy(alpha = 0.7f) else PulseMuted.copy(alpha = 0.2f),
                            center + axis * radius * 1.35f, center + axis * radius * if (tick % 6 == 0) 1.27f else 1.31f,
                            strokeWidth = if (tick % 6 == 0) 2f else 1f)
                    }
                    listOf(0.48f, 0.76f, 1f, 1.2f).forEach { scale ->
                        drawCircle(PulseViolet.copy(alpha = if (scale == 1f) 0.4f else 0.16f),
                            radius * scale, center, style = Stroke(1.dp.toPx()))
                    }
                    drawCircle(Brush.sweepGradient(listOf(PulseViolet, PulseViolet.copy(alpha = 0.03f),
                        PulseMint.copy(alpha = 0.8f), PulseViolet), center), radius, center, style = Stroke(2.dp.toPx()))
                    drawLine(PulseMint.copy(alpha = 0.12f), center - Offset(radius * 1.2f, 0f), center + Offset(radius * 1.2f, 0f))
                    drawLine(PulseMint.copy(alpha = 0.12f), center - Offset(0f, radius * 1.2f), center + Offset(0f, radius * 1.2f))
                    val beacon = center
                    drawCircle(PulseViolet.copy(alpha = 0.18f), 15.dp.toPx(), beacon)
                    drawCircle(PulseViolet, 5.dp.toPx(), beacon)
                    if (bearing != null && bearing[0].isFinite()) {
                        val angle = Math.toRadians(bearing[1].toDouble())
                        val relativeDistance = (bearing[0] / HotspotConfig.RADIUS_METERS).coerceIn(0f, 1.35f)
                        val point = center + Offset(sin(angle).toFloat(), -cos(angle).toFloat()) * radius * relativeDistance
                        val color = if (inRange) PulseMint else Color(0xFFFFCE86)
                        drawLine(color.copy(alpha = 0.5f), center, point, strokeWidth = 2f)
                        drawCircle(color.copy(alpha = 0.13f), 17.dp.toPx(), point)
                        drawCircle(color, 6.dp.toPx(), point)
                        drawCircle(Color.White, 6.dp.toPx(), point, style = Stroke(1.5.dp.toPx()))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PulseIcon(PulseGlyph.Pin, color = PulseMint, modifier = Modifier.size(28.dp))
                    Text("100", style = MaterialTheme.typography.displayLarge, color = Color.White)
                    Text("METER TEST RADIUS", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
                }
                listOf(
                    Triple("N", "North", Alignment.TopCenter),
                    Triple("E", "East", Alignment.CenterEnd),
                    Triple("S", "South", Alignment.BottomCenter),
                    Triple("W", "West", Alignment.CenterStart)
                ).forEach { (letter, direction, alignment) ->
                    Text(letter, Modifier.align(alignment).semantics { contentDescription = "$letter — $direction" },
                        color = PulseMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(if (hotspot == null) "AWAITING YOUR TEST ZONE" else "LOCAL HOTSPOT / GPS ON DEMAND",
                Modifier.fillMaxWidth(), color = PulseMuted, style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "UI06 dashboard - compact", widthDp = 360, heightDp = 780)
@androidx.compose.ui.tooling.preview.Preview(name = "UI06 dashboard - large text", widthDp = 320, heightDp = 700, fontScale = 1.5f)
@Composable
private fun DemoSetupPreview() { PulseTheme { Surface(color = PulseBackground) { MapScreen() } } }
