// Local meme editor with local Pulse Points rewards.
package com.pulse.mobile.ui.meme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.data.MemeRepository
import com.pulse.mobile.data.MintedMeme
import com.pulse.mobile.data.TokenManager
import com.pulse.mobile.data.HotspotType
import com.pulse.mobile.ui.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Models ---

data class MemeTemplate(
    val id: String,
    val emoji: String,
    val label: String,
    val bgColor: Color
)

// --- Main Editor Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeEditorScreen() {
    val scope = rememberCoroutineScope()
    val generation = remember { TokenManager.demoGeneration }

    val templates = remember {
        listOf(
            MemeTemplate("1", "🚀", "Moon Bro",      Color(0xFF1A1A2E)),
            MemeTemplate("2", "💎", "Diamond Hands", Color(0xFF16213E)),
            MemeTemplate("3", "🐸", "Pepe DeFi",     Color(0xFF0F3460)),
            MemeTemplate("4", "🤖", "AI Degen",      Color(0xFF1B1B2F)),
            MemeTemplate("5", "🦍", "Ape Together",  Color(0xFF162447)),
            MemeTemplate("6", "⚡", "Solana Speed",  Color(0xFF1A0533))
        )
    }

    var selectedIndex by remember { mutableStateOf(0) }
    var topText       by remember { mutableStateOf("") }
    var bottomText    by remember { mutableStateOf("") }
    var isMinting     by remember { mutableStateOf(false) }
    var mintSuccess   by remember { mutableStateOf(false) }
    var pointsEarned     by remember { mutableStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showSnackbar  by remember { mutableStateOf(false) }

    val mintScale by animateFloatAsState(
        targetValue = if (isMinting) 0.95f else 1f,
        animationSpec = tween(150),
        label = "mintScale"
    )

    val selected = templates[selectedIndex]
    val tileLabelStyle = MaterialTheme.typography.labelMedium
    val tileLabelHeight = with(LocalDensity.current) { (tileLabelStyle.lineHeight * 2).toDp() }
    val tileWidth = 108.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val tileHeight = 28.dp + 32.dp + 8.dp + tileLabelHeight

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PulseHeading("PULSE CREATIVE STUDIO", "Make something\nworth sharing.", "Your local meme canvas. Your own little pulse of creativity.")
        PulseBadge("LOCAL PUBLISHING / PULSE POINTS", Color(0xFFC4A2FF))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("01 / PICK YOUR ENERGY", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(templates) { idx, tmpl ->
                    val isSelected = idx == selectedIndex
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) PulseViolet else PulseOutline, label = "borderColor")
                    Column(Modifier.width(tileWidth).height(tileHeight).clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) Color(0xFF2A1B43) else PulseSurface)
                        .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(22.dp))
                        .clickable(role = Role.RadioButton) { selectedIndex = idx }
                        .semantics { this.selected = isSelected }.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulseTemplateIcon(tmpl.id, Modifier.size(32.dp), color = if (isSelected) PulseMint else Color(0xFFC4A2FF))
                        Box(Modifier.fillMaxWidth().height(tileLabelHeight), contentAlignment = Alignment.Center) {
                            Text(tmpl.label, style = tileLabelStyle, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("02 / YOUR CANVAS", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
                Text("PREVIEW", color = PulseMint, style = MaterialTheme.typography.labelSmall)
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4A246D), selected.bgColor, Color(0xFF153932))))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))) {
                Canvas(Modifier.matchParentSize()) {
                    val center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                    repeat(5) { i -> drawCircle(Color.White.copy(alpha = 0.06f), (70 + i * 36).dp.toPx(), center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())) }
                }
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PULSE / STUDIO", color = Color(0xFFD7C3F5), style = MaterialTheme.typography.labelSmall)
                    if (topText.isNotBlank()) Text(topText.uppercase(), color = Color.White,
                        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    PulseTemplateIcon(selected.id, Modifier.size(64.dp))
                    if (bottomText.isNotBlank()) Text(bottomText.uppercase(), color = Color.White,
                        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text("MADE ON YOUR DEVICE", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        PulsePanel {
            Text("03 / Find your words", color = Color.White, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = topText, onValueChange = { topText = it }, label = { Text("Top line", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("Set the scene…") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = bottomText, onValueChange = { bottomText = it }, label = { Text("Bottom line", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("Land the punchline…") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(16.dp))
        }
        PulsePanel(accent = PulseMint) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PulseIcon(PulseGlyph.Spark, color = PulseMint, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f)) {
                    Text("Create. Share. Collect.", style = MaterialTheme.typography.titleMedium)
                    Text("${TokenManager.currentTier.displayName} / ${TokenManager.currentTier.multiplier}× multiplier",
                        color = PulseMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("+${TokenManager.calculateReward(50, TokenManager.currentTier, HotspotType.STANDARD)} Pulse Points",
                color = PulseMint, style = MaterialTheme.typography.headlineMedium)
            Text("A local publishing reward. Your meme appears in the demo feed on this device.", color = PulseMuted)
            PulseAction(when { isMinting -> "Saving your creation…"; mintSuccess -> "Saved! +$pointsEarned points"; else -> "Share local meme" },
                onClick = {

                if (!isMinting && !mintSuccess) {
                    isMinting = true
                    scope.launch {
                        delay(1500)
                        val reward = try {
                            TokenManager.publishMemeReward(generation)
                        } catch (e: Exception) {
                            saveError = e.message ?: "Could not save local points"
                            isMinting = false
                            return@launch
                        }
                        pointsEarned = reward
                        mintSuccess = true
                        isMinting = false
                        showSnackbar = true
                        MemeRepository.mintedMemes.add(
                            0,
                            MintedMeme(
                                templateId    = selected.id,
                                templateEmoji = selected.emoji,
                                topText       = topText.ifBlank { selected.label },
                                bottomText    = bottomText.ifBlank { "Shared in local demo" },
                                pointsReward     = reward,
                                timestamp     = "just now"
                            )
                        )
                        delay(3000)
                        showSnackbar = false
                        mintSuccess = false
                    }
                }

                }, enabled = !isMinting, loading = isMinting,
                modifier = Modifier.scale(mintScale), glyph = if (mintSuccess) PulseGlyph.Check else PulseGlyph.Arrow,
                appearance = PulseActionStyle.Solid, completed = mintSuccess)
            saveError?.let { PulseNotice(it, error = true) }
            if (showSnackbar) PulseNotice("Local meme saved! +$pointsEarned Pulse Points. Shared in the demo feed.")
        }
        Text("Studio publishes locally, not on-chain. Memes in the local feed are held in memory for this app session.",
            color = PulseMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
    }
}
