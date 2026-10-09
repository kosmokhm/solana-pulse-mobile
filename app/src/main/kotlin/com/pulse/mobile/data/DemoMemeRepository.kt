package com.pulse.mobile.data

import androidx.compose.runtime.mutableStateListOf

data class MintedMeme(
    val templateId: String,
    val templateEmoji: String,
    val topText: String,
    val bottomText: String,
    val pointsReward: Int,
    val timestamp: String
)

// --- Global meme repository for SocialFeed ---

object MemeRepository {
    val mintedMemes = mutableStateListOf<MintedMeme>()
}

