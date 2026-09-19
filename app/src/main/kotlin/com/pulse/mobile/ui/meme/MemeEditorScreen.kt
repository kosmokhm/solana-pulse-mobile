// MemeEditorScreen.kt – MemePulse Editor: Web3-мем редактор з mint як cNFT та нарахуванням $SKR
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.data.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Models ---

data class MemeTemplate(
    val id: String,
    val emoji: String,
    val label: String,
    val bgColor: Color
)

data class MintedMeme(
    val templateId: String,
    val templateEmoji: String,
    val topText: String,
    val bottomText: String,
    val skrReward: Int,
    val timestamp: String
)

// --- Global meme repository for SocialFeed ---

object MemeRepository {
    val mintedMemes = mutableStateListOf<MintedMeme>()
}

// --- Main Editor Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeEditorScreen() {
    val scope = rememberCoroutineScope()

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
    var skrEarned     by remember { mutableStateOf(0) }
    var showSnackbar  by remember { mutableStateOf(false) }

    val mintScale by animateFloatAsState(
        targetValue = if (isMinting) 0.95f else 1f,
        animationSpec = tween(150),
        label = "mintScale"
    )

    val selected = templates[selectedIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "🎨 MemePulse Editor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Створи мем • Mint як cNFT • Заробляй \$SKR",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9945FF)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Template selector
        Text(
            text = "Оберіть шаблон",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(templates) { idx, tmpl ->
                val isSelected = idx == selectedIndex
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF9945FF) else Color(0xFF333355),
                    label = "borderColor"
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tmpl.bgColor)
                        .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { selectedIndex = idx },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tmpl.emoji, fontSize = 28.sp)
                        Text(
                            text = tmpl.label,
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Meme canvas preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(selected.bgColor, selected.bgColor.copy(alpha = 0.6f))
                    )
                )
                .border(1.dp, Color(0xFF9945FF).copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (topText.isNotBlank()) {
                    Text(
                        text = topText.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(selected.emoji, fontSize = 72.sp)
                if (bottomText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bottomText.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Text(
                text = "PulseMobile · Solana",
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text inputs
        OutlinedTextField(
            value = topText,
            onValueChange = { topText = it },
            label = { Text("Верхній текст", color = Color(0xFF9945FF)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Color(0xFF9945FF),
                unfocusedBorderColor = Color(0xFF333355),
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = Color(0xFF9945FF)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = bottomText,
            onValueChange = { bottomText = it },
            label = { Text("Нижній текст", color = Color(0xFF14F195)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Color(0xFF14F195),
                unfocusedBorderColor = Color(0xFF333355),
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = Color(0xFF14F195)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Mint button
        Button(
            onClick = {
                if (!isMinting && !mintSuccess) {
                    isMinting = true
                    scope.launch {
                        delay(1500)
                        val reward = TokenManager.mintMemeReward()
                        skrEarned = reward
                        mintSuccess = true
                        isMinting = false
                        showSnackbar = true
                        MemeRepository.mintedMemes.add(
                            0,
                            MintedMeme(
                                templateId    = selected.id,
                                templateEmoji = selected.emoji,
                                topText       = topText.ifBlank { selected.label },
                                bottomText    = bottomText.ifBlank { "Minted on Solana" },
                                skrReward     = reward,
                                timestamp     = "just now"
                            )
                        )
                        delay(3000)
                        showSnackbar = false
                        mintSuccess = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(mintScale),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    mintSuccess -> Color(0xFF14F195)
                    isMinting   -> Color(0xFF9945FF).copy(alpha = 0.6f)
                    else        -> Color(0xFF9945FF)
                }
            ),
            enabled = !isMinting
        ) {
            when {
                isMinting   -> {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Minting cNFT на Solana…", color = Color.White, fontWeight = FontWeight.Bold)
                }
                mintSuccess -> Text("✅ Заминчено! +$skrEarned \$SKR", color = Color(0xFF0D0D1A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                else        -> Text("🪙 Mint as cNFT & Share (+50 \$SKR)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Tier info
        Spacer(modifier = Modifier.height(12.dp))
        val tier = TokenManager.currentTier
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF9945FF).copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Твій тір: ${tier.displayName}", color = Color(0xFF9945FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("x${tier.multiplier} бонус \$SKR", color = Color(0xFF14F195), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Success snackbar
        if (showSnackbar) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF14F195).copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎉 Мем заминчено як cNFT! Нараховано +$skrEarned \$SKR. Опубліковано у SocialFi стрічці!",
                    modifier = Modifier.padding(14.dp),
                    color = Color(0xFF14F195),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
