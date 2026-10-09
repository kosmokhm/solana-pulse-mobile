package com.pulse.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared native vector vocabulary; no icon-font or external dependency. */
enum class PulseGlyph { Radar, Rewards, Social, Meme, Wallet, Pin, Arrow, Shield, Check, Refresh, Spark, Heart, Moon, Diamond, Frog, Robot, Ape }

private val vectors: Map<PulseGlyph, ImageVector> = PulseGlyph.entries.associateWith { glyph ->
    ImageVector.Builder(glyph.name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            when (glyph) {
                PulseGlyph.Radar -> {
                    moveTo(21f,12f); curveTo(21f,17f,17f,21f,12f,21f); curveTo(7f,21f,3f,17f,3f,12f)
                    curveTo(3f,7f,7f,3f,12f,3f); curveTo(14f,3f,16f,4f,17f,5f)
                    moveTo(16f,12f); curveTo(16f,15f,14f,16f,12f,16f); curveTo(9f,16f,8f,14f,8f,12f)
                    curveTo(8f,9f,10f,8f,12f,8f); moveTo(12f,12f); lineTo(21f,3f)
                }
                PulseGlyph.Rewards -> { moveTo(12f,3f); lineTo(15f,8f); lineTo(21f,9f); lineTo(17f,14f); lineTo(18f,21f); lineTo(12f,18f); lineTo(6f,21f); lineTo(7f,14f); lineTo(3f,9f); lineTo(9f,8f); close() }
                PulseGlyph.Social -> { moveTo(3f,3f); lineTo(21f,3f); lineTo(21f,17f); lineTo(9f,17f); lineTo(4f,21f); lineTo(4f,17f); lineTo(3f,17f); close(); moveTo(7f,9f); lineTo(17f,9f); moveTo(7f,13f); lineTo(13f,13f) }
                // Same 3..21 bounds as the other navigation glyphs; wider smile + plus.
                PulseGlyph.Meme -> {
                    moveTo(12f,3f); lineTo(8f,3f); curveTo(5f,3f,3f,5f,3f,8f)
                    lineTo(3f,16f); curveTo(3f,19f,5f,21f,8f,21f); lineTo(16f,21f)
                    curveTo(19f,21f,21f,19f,21f,16f); lineTo(21f,12f)
                    moveTo(8f,9f); lineTo(8f,10f); moveTo(14f,9f); lineTo(14f,10f)
                    moveTo(8f,14f); curveTo(10f,18f,14f,18f,16f,14f)
                    moveTo(18f,3f); lineTo(18f,9f); moveTo(15f,6f); lineTo(21f,6f)
                }
                PulseGlyph.Wallet -> { moveTo(20f,7f); lineTo(4f,7f); lineTo(4f,21f); lineTo(21f,21f); lineTo(21f,7f); moveTo(4f,7f); lineTo(4f,4f); lineTo(17f,2f); lineTo(17f,7f); moveTo(21f,12f); lineTo(16f,12f); lineTo(16f,16f); lineTo(21f,16f) }
                PulseGlyph.Pin -> { moveTo(12f,22f); curveTo(9f,18f,5f,14f,5f,9f); curveTo(5f,0f,19f,0f,19f,9f); curveTo(19f,14f,15f,18f,12f,22f); moveTo(15f,9f); curveTo(15f,13f,9f,13f,9f,9f); curveTo(9f,5f,15f,5f,15f,9f) }
                PulseGlyph.Arrow -> { moveTo(4f,12f); lineTo(20f,12f); moveTo(14f,6f); lineTo(20f,12f); lineTo(14f,18f) }
                PulseGlyph.Shield -> { moveTo(12f,2f); lineTo(21f,6f); lineTo(20f,15f); curveTo(19f,18f,15f,21f,12f,22f); curveTo(9f,21f,5f,18f,4f,15f); lineTo(3f,6f); close(); moveTo(8f,12f); lineTo(11f,15f); lineTo(16f,9f) }
                PulseGlyph.Check -> { moveTo(5f,12f); lineTo(10f,17f); lineTo(20f,6f) }
                PulseGlyph.Refresh -> { moveTo(20f,9f); curveTo(17f,0f,3f,2f,3f,12f); curveTo(3f,23f,19f,24f,21f,14f); moveTo(20f,3f); lineTo(20f,9f); lineTo(14f,9f) }
                PulseGlyph.Heart -> {
                    moveTo(12f,21f); lineTo(4.5f,13f); curveTo(-1f,6f,8f,0f,12f,7f)
                    curveTo(16f,0f,25f,6f,19.5f,13f); close()
                }
                PulseGlyph.Moon -> {
                    moveTo(21f,13f); curveTo(19f,21f,9f,24f,4f,16f)
                    curveTo(-1f,8f,6f,1f,12f,3f); curveTo(5f,10f,14f,19f,21f,13f); close()
                }
                PulseGlyph.Diamond -> {
                    moveTo(3f,8f); lineTo(7f,3f); lineTo(17f,3f); lineTo(21f,8f); lineTo(12f,21f); close()
                    moveTo(3f,8f); lineTo(21f,8f); moveTo(8f,8f); lineTo(12f,21f); lineTo(16f,8f)
                }
                PulseGlyph.Frog -> {
                    moveTo(4f,11f); curveTo(0f,2f,10f,0f,10f,9f); lineTo(14f,9f)
                    curveTo(14f,0f,24f,2f,20f,11f); curveTo(26f,22f,-2f,22f,4f,11f)
                    moveTo(6f,7f); lineTo(6f,8f); moveTo(18f,7f); lineTo(18f,8f)
                    moveTo(8f,14f); curveTo(10f,17f,14f,17f,16f,14f)
                }
                PulseGlyph.Robot -> {
                    moveTo(4f,7f); lineTo(20f,7f); lineTo(20f,21f); lineTo(4f,21f); close()
                    moveTo(12f,7f); lineTo(12f,2f); moveTo(9f,12f); lineTo(9f,13f)
                    moveTo(15f,12f); lineTo(15f,13f); moveTo(8f,17f); lineTo(16f,17f)
                    moveTo(1f,11f); lineTo(1f,17f); moveTo(23f,11f); lineTo(23f,17f)
                }
                PulseGlyph.Ape -> {
                    moveTo(5f,10f); curveTo(-1f,6f,-1f,18f,5f,16f)
                    curveTo(6f,24f,18f,24f,19f,16f); curveTo(25f,18f,25f,6f,19f,10f)
                    curveTo(20f,0f,4f,0f,5f,10f); moveTo(8f,10f); lineTo(8f,11f)
                    moveTo(16f,10f); lineTo(16f,11f); moveTo(8f,16f); curveTo(10f,18f,14f,18f,16f,16f)
                }
                PulseGlyph.Spark -> { moveTo(14f,2f); lineTo(5f,14f); lineTo(11f,14f); lineTo(10f,22f); lineTo(20f,9f); lineTo(13f,9f); close() }
            }
        }
    }.build()
}

@Composable
fun PulseIcon(glyph: PulseGlyph, modifier: Modifier = Modifier, color: Color = PulseMuted, description: String? = null) {
    Icon(vectors.getValue(glyph), description, modifier.size(24.dp), tint = color)
}

@Composable
fun PulseBadge(text: String, color: Color = PulseMint) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f))) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color,
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun PulsePanel(modifier: Modifier = Modifier, accent: Color = PulseViolet,
               verticalPadding: Dp = 18.dp, spacing: Dp = 10.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = PulseSurface,
        border = BorderStroke(1.dp, PulseOutline)) {
        Column(Modifier.background(Brush.linearGradient(listOf(accent.copy(alpha = 0.07f), Color.Transparent)))
            .padding(horizontal = 18.dp, vertical = verticalPadding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}

@Composable
fun PulseHeading(kicker: String, title: String, description: String? = null, section: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(kicker.uppercase(), color = PulseMint, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
        Text(title, style = if (section) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge)
        description?.let { Text(it, color = PulseMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

enum class PulseActionStyle { Gradient, Outline, Solid }

@Composable
fun PulseAction(text: String, onClick: () -> Unit, enabled: Boolean = true, loading: Boolean = false,
                modifier: Modifier = Modifier, glyph: PulseGlyph = PulseGlyph.Arrow,
                appearance: PulseActionStyle = PulseActionStyle.Gradient, completed: Boolean = false) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        completed -> Brush.horizontalGradient(listOf(PulseMint.copy(alpha = 0.08f), PulseMint.copy(alpha = 0.08f)))
        !enabled -> Brush.horizontalGradient(listOf(PulseOutline.copy(alpha = 0.55f), PulseOutline.copy(alpha = 0.55f)))
        pressed -> Brush.horizontalGradient(listOf(PulseViolet, Color(0xFF6954EF)))
        appearance == PulseActionStyle.Outline -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
        appearance == PulseActionStyle.Solid -> Brush.horizontalGradient(listOf(Color(0xFF392454), Color(0xFF392454)))
        else -> Brush.horizontalGradient(listOf(PulseViolet, Color(0xFF6954EF)))
    }
    val ink = if (completed) PulseMint else if (enabled) Color.White else PulseMuted
    val edge = if (completed) PulseMint.copy(alpha = 0.3f) else if (enabled) Color(0xFFB28AFF).copy(alpha = 0.65f) else PulseOutline
    Button(onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.fillMaxWidth().heightIn(min = if (appearance == PulseActionStyle.Gradient) 64.dp else 56.dp)
            .clip(shape).background(fill), shape = shape, border = BorderStroke(1.dp, edge),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent,
            contentColor = ink, disabledContentColor = ink), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(12.dp))
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = ink, strokeWidth = 2.dp)
        else PulseIcon(glyph, color = ink)
    }
}

/** Pure presentation mapping. Template ids/emoji metadata and publish payload are unchanged. */
@Composable
fun PulseTemplateIcon(templateId: String, modifier: Modifier = Modifier, color: Color = PulseMint) {
    val glyph = when (templateId) {
        "1" -> PulseGlyph.Moon
        "2" -> PulseGlyph.Diamond
        "3" -> PulseGlyph.Frog
        "4" -> PulseGlyph.Robot
        "5" -> PulseGlyph.Ape
        "6" -> PulseGlyph.Spark
        else -> PulseGlyph.Meme
    }
    PulseIcon(glyph, modifier, color)
}

@Composable
fun PulseNotice(message: String, error: Boolean = false) {
    val color = if (error) MaterialTheme.colorScheme.error else PulseMint
    Surface(shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PulseIcon(if (error) PulseGlyph.Shield else PulseGlyph.Check, color = color)
            Text(message, color = color, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PulseStat(label: String, value: String, glyph: PulseGlyph, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color(0xFF101627), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PulseOutline)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PulseIcon(glyph, color = PulseMint, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, color = PulseMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PulseBalanceCard(balance: Int, subtitle: String, compact: Boolean = false) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF5326A4), Color(0xFF302354), Color(0xFF15352F))))
        .background(Brush.verticalGradient(listOf(PulseViolet.copy(alpha = 0.18f), Color.Transparent)))) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.2f)
            repeat(4) { index ->
                drawCircle(Color.White.copy(alpha = 0.055f), 35.dp.toPx() + index * 24.dp.toPx(), center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = if (compact) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR BALANCE", color = Color(0xFFD6C8F0), style = MaterialTheme.typography.labelSmall)
                PulseBadge("LOCAL DEMO")
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val scale = LocalDensity.current.fontScale
                val size = minOf(if (compact) 44f else 64f, maxWidth.value / (balance.toString().length * 0.66f * scale))
                Text(balance.toString(), style = MaterialTheme.typography.displayLarge,
                    fontSize = size.sp, lineHeight = (size + 6).sp)
            }
            Text("Pulse Points", style = MaterialTheme.typography.titleMedium, color = PulseMint)
            if (subtitle.isNotBlank()) Text(subtitle, color = Color(0xFFC2BED5), style = MaterialTheme.typography.bodySmall)
        }
    }
}
