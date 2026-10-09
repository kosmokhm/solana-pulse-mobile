// SocialFeedScreen.kt – SocialFi network activity feed
package com.pulse.mobile.ui.feed

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.mobile.data.TokenManager
import com.pulse.mobile.ui.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import com.pulse.mobile.data.MemeRepository
import com.pulse.mobile.data.MintedMeme

data class SocialFeedPost(
    val id: String,
    val userName: String,
    val userHandle: String,
    val avatarEmoji: String,
    val locationName: String,
    val rewardPoints: Int,
    val timestamp: String,
    val initialLikes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen() {
    val posts = remember {
        mutableStateListOf(
            SocialFeedPost("1", "Alex Solana", "alex.sol", "⚡", "Solana Mobile Hub", 75, "2 mins ago", 14),
            SocialFeedPost("2", "Elena Web3", "elena.sol", "🚀", "Genesis Block Cafe", 38, "12 mins ago", 8),
            SocialFeedPost("3", "Seeker_Pro", "seeker99.sol", "👾", "Proof of History Zone", 150, "45 mins ago", 29),
            SocialFeedPost("4", "CryptoDegen", "degen.sol", "💎", "Web3 Solana Park", 60, "1 hour ago", 19)
        )
    }

    var selectedFilter by remember { mutableStateOf("All Activity") }
    val filters = listOf("All Activity", "Top Earners", "Hotspots", "Memes 🎨")
    val memePosts = MemeRepository.mintedMemes

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PulseHeading("THE PULSE COLLECTIVE", "Your orbit.\nYour people.", "Sample community stories and memes shared on this device.") }
        item {
            PulsePanel {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PulseIcon(PulseGlyph.Social, color = Color(0xFFC4A2FF), modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("A glimpse of the collective", style = MaterialTheme.typography.titleMedium)
                        Text("Sample stories and local memes. No live network feed.", color = PulseMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                PulseBadge("DEMO FEED / NO LIVE DATA", Color(0xFFC4A2FF))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(selected = selectedFilter == filter, onClick = { selectedFilter = filter },
                        label = { Text(if (filter == "Memes 🎨") "Your memes" else filter) },
                        modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PulseViolet.copy(alpha = 0.2f), selectedLabelColor = Color.White))
                }
            }
        }
        if (selectedFilter == "All Activity" || selectedFilter == "Memes 🎨") {
            itemsIndexed(memePosts) { _, meme -> MemeFeedCard(meme) }
            if (selectedFilter == "Memes 🎨" && memePosts.isEmpty()) item {
                PulsePanel(accent = PulseMint) {
                    PulseIcon(PulseGlyph.Meme, color = PulseMint, modifier = Modifier.size(40.dp))
                    Text("Your story starts in Studio.", style = MaterialTheme.typography.titleMedium)
                    Text("Share a local meme from the Studio tab to see it here. Nothing is uploaded or minted.", color = PulseMuted)
                }
            }
        }
        if (selectedFilter != "Memes 🎨") {
            items(posts, key = { it.id }) { FeedPostCard(it) }
        }
        item { Text("Local demo only. Points do not pay another wallet or author.",
            color = PulseMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun FeedPostCard(post: SocialFeedPost) {
    var likeCount by remember { mutableStateOf(post.initialLikes) }
    var isLiked by remember { mutableStateOf(false) }
    val heartColor by animateColorAsState(targetValue = if (isLiked) Color(0xFFFF8AB0) else PulseMuted, label = "heartColor")
    PulsePanel(verticalPadding = 12.dp, spacing = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(PulseViolet.copy(alpha = 0.17f)),
                contentAlignment = Alignment.Center) { Text(post.avatarEmoji, fontSize = 21.sp) }
            Column(Modifier.weight(1f)) {
                Text(post.userName, style = MaterialTheme.typography.titleMedium)
                Text("@${post.userHandle}", color = PulseMuted, style = MaterialTheme.typography.bodySmall)
            }
            PulseBadge("SAMPLE", Color(0xFFC4A2FF))
        }
        Text("FOUND A NEW PULSE", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
        Text(post.locationName, style = MaterialTheme.typography.titleMedium)
        Surface(color = Color(0xFF10352E), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("+${post.rewardPoints} Pulse Points", color = PulseMint, style = MaterialTheme.typography.titleLarge)
                Text("Illustrative sample reward • Not your balance", color = PulseMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = {

                    if (isLiked) {
                        isLiked = false
                        likeCount--
                    } else {
                        isLiked = true
                        likeCount++
                    }

            }, modifier = Modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                PulseIcon(PulseGlyph.Heart, color = heartColor)
                Spacer(Modifier.width(8.dp))
                Text("$likeCount likes", color = heartColor, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.weight(1f))
            Text("Sample story", color = PulseMuted, style = MaterialTheme.typography.bodySmall,
                letterSpacing = 0.sp, modifier = Modifier.padding(end = 4.dp))
        }
        OutlinedButton(onClick = { /* Boost action */ }, enabled = false,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            shape = RoundedCornerShape(16.dp)) { Text("Boost unavailable in demo", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
fun MemeFeedCard(meme: MintedMeme) {
    var tipCount by remember { mutableStateOf(0) }
    var tipped by remember { mutableStateOf(false) }
    var tipError by remember { mutableStateOf(false) }
    PulsePanel(verticalPadding = 14.dp, spacing = 8.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            PulseBadge("FROM YOUR STUDIO", Color(0xFFC4A2FF))
            Text("LOCAL", color = PulseMuted, style = MaterialTheme.typography.labelSmall)
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF342058), Color(0xFF132D2B))))) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (meme.topText.isNotBlank()) Text(meme.topText.uppercase(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge)
                Surface(shape = RoundedCornerShape(16.dp), color = PulseSurface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, PulseOutline)) {
                    Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        PulseTemplateIcon(meme.templateId, Modifier.size(48.dp))
                    }
                }
                if (meme.bottomText.isNotBlank()) Text(meme.bottomText.uppercase(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge)
            }
        }
        Text("+${meme.pointsReward} Pulse Points", color = PulseMint, style = MaterialTheme.typography.titleMedium)
        Text("Local reward from publishing this meme", color = PulseMuted, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
                        val success = runCatching { TokenManager.tipMeme(5) }.getOrDefault(false)
                        if (success) {
                            tipCount++
                            tipped = true
                            tipError = false
                        } else {
                            tipError = true
                        }

        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
            PulseIcon(PulseGlyph.Spark, color = if (tipped) PulseMint else Color(0xFFC4A2FF))
            Spacer(Modifier.width(10.dp))
            Text("Spend 5 local points${if (tipCount > 0) " ($tipCount)" else ""}", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        if (tipError) PulseNotice("Cannot spend local points", error = true)
    }
}
