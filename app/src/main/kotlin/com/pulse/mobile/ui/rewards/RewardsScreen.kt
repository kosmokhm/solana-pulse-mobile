// RewardsScreen.kt – Seeker Tier dashboard, Pulse Points balance, and boosts breakdown
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
import com.pulse.mobile.ui.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pulse.mobile.data.SeekerTier
import com.pulse.mobile.data.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen() {
    var claimRefresh by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            claimRefresh++
        }
    }
    val streakClaimed = remember(claimRefresh, TokenManager.dailyClaimed) { TokenManager.dailyClaimed }
    var streakMessage by remember { mutableStateOf<String?>(null) }

    val currentTier = TokenManager.currentTier
    val nextTier = TokenManager.nextTier
    val progress = TokenManager.xpProgressToNextTier

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PulseHeading("PROGRESS, NOT PROMISES", "Your effort.\nYour rewards.", "Every point here belongs to your local demo ledger.") }
        item { PulseBalanceCard(TokenManager.balancePoints, "Local points. No token value.", compact = true) }
        item {
            PulsePanel {
                Text("YOUR SEEKER IDENTITY", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawArc(PulseOutline, -90f, 360f, false, style = Stroke(6.dp.toPx()))
                            drawArc(PulseMint, -90f, 360f * animatedProgress, false, style = Stroke(6.dp.toPx()))
                        }
                        PulseIcon(PulseGlyph.Rewards, color = PulseMint, modifier = Modifier.size(32.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(currentTier.displayName, style = MaterialTheme.typography.titleMedium)
                        Text("${TokenManager.userXp} XP / ${currentTier.multiplier}× boost", color = PulseMint,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(nextTier?.let { "Next: ${it.displayName} at ${it.requiredXp} XP" } ?: "Highest tier reached",
                            color = PulseMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            PulsePanel(accent = PulseMint) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PulseIcon(PulseGlyph.Spark, color = PulseMint, modifier = Modifier.size(32.dp))
                    PulseBadge("DAILY / UTC")
                }
                Text("A little momentum.", style = MaterialTheme.typography.titleMedium)
                Text("+20 Pulse Points", color = PulseMint, style = MaterialTheme.typography.headlineMedium)
                Text(if (streakClaimed) "Collected for today. Come back on the next UTC day."
                    else "One daily bonus. No change to your check-in count or XP.", color = PulseMuted)
                PulseAction(if (streakClaimed) "Collected for today" else "Collect daily bonus",
                    onClick = {

                            if (!streakClaimed) {
                                try {
                                    if (TokenManager.claimDailyBonus()) {
                                        streakMessage = "Claimed +20 Pulse Points daily bonus!"
                                    }
                                } catch (e: Exception) {
                                    streakMessage = e.message ?: "Could not save daily bonus"
                                }
                                claimRefresh++
                            }

                    }, enabled = !streakClaimed, glyph = if (streakClaimed) PulseGlyph.Check else PulseGlyph.Arrow,
                    appearance = PulseActionStyle.Outline, completed = streakClaimed)
                streakMessage?.let { PulseNotice(it, error = !it.startsWith("Claimed")) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PulseStat("Check-ins", TokenManager.totalCheckIns.toString(), PulseGlyph.Pin, Modifier.weight(1f))
                PulseStat("Memes created", TokenManager.totalMemes.toString(), PulseGlyph.Meme, Modifier.weight(1f))
            }
        }
        item { PulseHeading("THE SEEKER PATH", "Level up your pulse.", section = true) }
        items(SeekerTier.entries) { tier ->
            val active = tier == currentTier
            PulsePanel(accent = if (active) PulseMint else PulseViolet) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(if (active) PulseMint.copy(alpha = 0.12f) else PulseOutline),
                        contentAlignment = Alignment.Center) {
                        if (active) PulseIcon(PulseGlyph.Check, color = PulseMint)
                        else Text("0${tier.ordinal + 1}", color = PulseMuted, style = MaterialTheme.typography.labelLarge)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(tier.displayName, style = MaterialTheme.typography.titleMedium)
                        Text("${tier.requiredXp} XP to unlock", color = PulseMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${tier.multiplier.toString().removeSuffix(".0")}×",
                        modifier = Modifier.widthIn(min = 56.dp),
                        color = if (active) PulseMint else Color(0xFFC4A2FF),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End, softWrap = false)
                }
                if (active) PulseBadge("YOUR CURRENT TIER")
            }
        }
        item { Text("Pulse Points are local demo points. Tier multipliers do not represent financial returns.",
            color = PulseMuted, style = MaterialTheme.typography.bodySmall) }
    }
}
