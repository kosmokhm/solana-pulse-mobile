// RewardsScreen.kt – Seeker Tier dashboard, $SKR balance, and boosts breakdown
package com.pulse.mobile.ui.rewards

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.data.SeekerTier
import com.pulse.mobile.data.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen() {
    var streakClaimed by remember { mutableStateOf(false) }
    var streakMessage by remember { mutableStateOf<String?>(null) }

    val currentTier = TokenManager.currentTier
    val nextTier = TokenManager.nextTier
    val progress = TokenManager.xpProgressToNextTier

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Card with Solana-style Gradient
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF9945FF), Color(0xFF14F195))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Your \$SKR Balance",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${TokenManager.balanceSkr}",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "\$SKR",
                                color = Color(0xFF14F195),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Check-Ins", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Text("${TokenManager.totalCheckIns}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Multiplier", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Text("${currentTier.multiplier}x", color = Color(0xFF14F195), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        // Seeker Tier Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9945FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏆", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentTier.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current Tier Boost: ${currentTier.multiplier}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF14F195).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${TokenManager.userXp} XP",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color(0xFF14F195),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF14F195),
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (nextTier != null) {
                            "Next: ${nextTier.displayName} (${nextTier.requiredXp} XP)"
                        } else {
                            "Maximum Seeker Tier Reached! 🎉"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Daily Streak Bonus Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ Daily Streak Bonus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (streakClaimed) "Claimed for today! +20 \$SKR" else "Claim +20 \$SKR daily reward",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            if (!streakClaimed) {
                                streakClaimed = true
                                TokenManager.processCheckIn(20, com.pulse.mobile.data.HotspotType.STANDARD)
                                streakMessage = "Claimed +20 \$SKR daily bonus!"
                            }
                        },
                        enabled = !streakClaimed
                    ) {
                        Text(if (streakClaimed) "Claimed ✓" else "Claim")
                    }
                }
            }
        }

        // Seeker Tiers Roadmap / Perks List
        item {
            Text(
                text = "Seeker Tiers Perks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(SeekerTier.entries) { tier ->
            val isCurrent = tier == currentTier
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = tier.displayName,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${tier.multiplier}x Reward Multiplier • Requires ${tier.requiredXp} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
