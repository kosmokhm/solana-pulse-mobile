// MainScreen composable with wallet connection, map, rewards, and SocialFi navigation
package com.pulse.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.pulse.mobile.data.TokenManager
import com.pulse.mobile.ui.feed.SocialFeedScreen
import com.pulse.mobile.ui.map.MapScreen
import com.pulse.mobile.ui.meme.MemeEditorScreen
import com.pulse.mobile.ui.rewards.RewardsScreen

enum class MainTab(val title: String, val icon: String) {
    MAP("Map", "🗺️"),
    REWARDS("Rewards", "🎁"),
    FEED("SocialFi", "🌐"),
    MEME("Memes", "🎨")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(connectWallet: suspend () -> Result<String>) {
    var walletAddress by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(MainTab.MAP) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pulse Mobile",
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "⚡ ${TokenManager.balanceSkr} \$SKR",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (walletAddress == null) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val result = connectWallet()
                                            if (result.isSuccess) {
                                                walletAddress = result.getOrNull()
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Connect Wallet")
                                }
                            } else {
                                val shortKey = walletAddress?.let { it.take(4) + "..." + it.takeLast(4) } ?: ""
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "🔑 $shortKey",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.MAP     -> MapScreen()
                MainTab.REWARDS -> RewardsScreen()
                MainTab.FEED    -> SocialFeedScreen()
                MainTab.MEME    -> MemeEditorScreen()
            }
        }
    }
}


