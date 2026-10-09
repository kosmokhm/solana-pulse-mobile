package com.pulse.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.pulse.mobile.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.mwa.WalletUiState
import com.pulse.mobile.mwa.WalletStatus
import com.pulse.mobile.data.TokenManager
import com.pulse.mobile.ui.debug.DeveloperDemoTools
import com.pulse.mobile.ui.feed.SocialFeedScreen
import com.pulse.mobile.ui.map.MapScreen
import com.pulse.mobile.ui.meme.MemeEditorScreen
import com.pulse.mobile.ui.rewards.RewardsScreen

enum class MainTab(val title: String, val glyph: PulseGlyph) {
    MAP("Explore", PulseGlyph.Radar), REWARDS("Rewards", PulseGlyph.Rewards),
    FEED("SocialFi", PulseGlyph.Social), MEME("Studio", PulseGlyph.Meme)
}

@Composable
fun MainScreen(
    walletState: WalletUiState = WalletUiState(WalletStatus.DISCONNECTED),
    connectWallet: () -> Unit = {}, disconnectWallet: () -> Unit = {},
    acceptWalletChange: () -> Unit = {}, rejectWalletChange: () -> Unit = {}
) {
    val walletAddress = walletState.address
    val connecting = walletState.busy
    var showWallet by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.MAP) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(walletState.message) { walletState.message?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(walletState.status) {
        if (walletState.status == WalletStatus.DISCONNECTED || walletState.status == WalletStatus.ACCOUNT_CHANGED)
            showWallet = false
    }
    val walletClick: () -> Unit = {
        if (!connecting) {
            if (walletState.status == WalletStatus.DISCONNECTED) connectWallet()
            else showWallet = true
        }
    }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(containerColor = PulseBackground,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(Modifier.fillMaxWidth().background(PulseBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClickLabel = "Open Explore") { selectedTab = MainTab.MAP }
                    .semantics(mergeDescendants = true) { contentDescription = "Pulse Mobile. Clock in. Local demo. Open Explore" }
                    .padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(
                        Brush.linearGradient(listOf(PulseViolet, Color(0xFF50329C)))), contentAlignment = Alignment.Center) {
                        PulseIcon(PulseGlyph.Spark, color = PulseMint)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Pulse Mobile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("CLOCK IN • LOCAL DEMO", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(walletClick, enabled = !connecting, modifier = Modifier
                    .size(48.dp).clip(CircleShape).background(PulseSurface).border(1.dp, PulseOutline, CircleShape)) {
                    if (connecting) CircularProgressIndicator(Modifier.size(20.dp), color = PulseMint, strokeWidth = 2.dp)
                    else PulseIcon(PulseGlyph.Wallet, color = if (walletAddress != null) PulseMint else Color.White,
                        description = if (walletAddress != null) "View connected wallet" else "Connect Solana wallet")
                }
                Spacer(Modifier.width(8.dp))
                if (BuildConfig.DEBUG) Box(Modifier.size(48.dp).clip(CircleShape)
                    .background(PulseSurface).border(1.dp, PulseOutline, CircleShape), contentAlignment = Alignment.Center) {
                    DeveloperDemoTools(onReset = { selectedTab = MainTab.MAP })
                }
            }
        }, bottomBar = { if (!keyboardVisible) PulseNavigation(selectedTab) { selectedTab = it } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            // Recreate only demo screens after an explicit reset; wallet session stays intact.
            key(TokenManager.demoGeneration) {
            when (selectedTab) {
                MainTab.MAP -> MapScreen(walletContent = { WalletSummary(walletAddress, connecting, walletClick, walletState.label) })
                MainTab.REWARDS -> RewardsScreen()
                MainTab.FEED -> SocialFeedScreen()
                MainTab.MEME -> MemeEditorScreen()
            }
            }
        }
    }
    if (showWallet && walletState.status != WalletStatus.ACCOUNT_CHANGED) AlertDialog(
        onDismissRequest = { showWallet = false },
        icon = { PulseIcon(PulseGlyph.Wallet, color = PulseMint) },
        title = { Text(if (walletAddress != null) "Connected wallet" else walletState.label) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val shownAddress = walletAddress ?: walletState.rememberedAddress
                shownAddress?.let {
                    if (walletAddress == null) Text("Last wallet • not verified", color = PulseMuted)
                    SelectionContainer { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
                Text(if (walletAddress != null)
                    "Real Mobile Wallet Adapter authorization. Your Pulse Points are a separate local demo balance."
                    else "Connection is not confirmed. Reconnect through your wallet to continue.", color = PulseMuted)
            }
        }, confirmButton = {
            if (walletAddress != null) TextButton(onClick = { showWallet = false }) { Text("Done") }
            else TextButton(enabled = !connecting, onClick = { showWallet = false; connectWallet() }) { Text("Reconnect wallet") }
        }, dismissButton = {
            TextButton(enabled = !connecting, onClick = disconnectWallet) { Text("Disconnect") }
        })
    if (walletState.status == WalletStatus.ACCOUNT_CHANGED) AlertDialog(
        onDismissRequest = rejectWalletChange,
        title = { Text("Confirm wallet change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The wallet returned a different account or provider. Nothing has been switched yet.")
                if (walletState.providerChanged) Text("Wallet provider changed.", color = PulseMuted)
                Text("Previous", color = PulseMuted)
                SelectionContainer { Text(walletState.rememberedAddress.orEmpty()) }
                Text("Proposed", color = PulseMuted)
                SelectionContainer { Text(walletState.candidateAddress.orEmpty()) }
            }
        }, confirmButton = { TextButton(onClick = acceptWalletChange) { Text("Use this wallet") } },
        dismissButton = { TextButton(onClick = rejectWalletChange) { Text("Keep previous wallet") } })
}

@Composable
private fun WalletSummary(address: String?, connecting: Boolean, onClick: () -> Unit, status: String) {
    Surface(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .clickable(enabled = !connecting, role = Role.Button, onClick = onClick).semantics {
            contentDescription = if (address == null) status else "Connected wallet. View full address"
        }, shape = RoundedCornerShape(22.dp), color = PulseSurface,
        border = BorderStroke(1.dp, PulseOutline)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(PulseViolet.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) { PulseIcon(PulseGlyph.Wallet, color = Color(0xFFBE94FF)) }
            Column(Modifier.weight(1f)) {
                Text(if (address == null) "Your Solana wallet" else address.take(6) + "…" + address.takeLast(6),
                    style = MaterialTheme.typography.titleMedium)
                Text(status,
                    color = if (address != null) PulseMint else PulseMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (connecting) CircularProgressIndicator(Modifier.size(20.dp), color = PulseMint, strokeWidth = 2.dp)
            else PulseIcon(if (address == null) PulseGlyph.Arrow else PulseGlyph.Check, color = PulseMint)
        }
    }
}

@Composable
private fun PulseNavigation(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, PulseBackground)))
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        .padding(horizontal = 16.dp, vertical = 10.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = PulseSurface,
            border = BorderStroke(1.dp, PulseOutline), shadowElevation = 14.dp) {
            Row(Modifier.fillMaxWidth().padding(6.dp).selectableGroup()) {
                MainTab.entries.forEach { tab ->
                    val active = tab == selected
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                        .background(if (active) PulseViolet.copy(alpha = 0.17f) else Color.Transparent)
                        .selectable(active, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        PulseIcon(tab.glyph, color = if (active) PulseMint else PulseMuted)
                        Text(tab.title, color = if (active) Color.White else PulseMuted,
                            style = MaterialTheme.typography.labelSmall, letterSpacing = 0.sp)
                        Box(Modifier.size(width = 12.dp, height = 3.dp).background(
                            if (active) PulseMint else Color.Transparent, CircleShape))
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "UI07 - full dashboard", widthDp = 393, heightDp = 852)
@androidx.compose.ui.tooling.preview.Preview(name = "UI07 - compact large text", widthDp = 320, heightDp = 700, fontScale = 1.5f)
@Composable
private fun DashboardPreview() {
    PulseTheme { MainScreen() }
}
