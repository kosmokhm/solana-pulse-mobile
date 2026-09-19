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
import com.pulse.mobile.ui.meme.MemeRepository
import com.pulse.mobile.ui.meme.MintedMeme

data class SocialFeedPost(
    val id: String,
    val userName: String,
    val userHandle: String,
    val avatarEmoji: String,
    val locationName: String,
    val rewardSkr: Int,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "SocialFi Feed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Live network check-ins & \$SKR rewards",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Feed List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Meme posts section
            if (selectedFilter == "All Activity" || selectedFilter == "Memes 🎨") {
                items(memePosts, key = { "meme_${it.timestamp}_${it.templateId}" }) { meme ->
                    MemeFeedCard(meme = meme)
                }
            }
            // Regular check-in posts
            if (selectedFilter != "Memes 🎨") {
                items(posts, key = { it.id }) { post ->
                    FeedPostCard(post = post)
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(post: SocialFeedPost) {
    var likeCount by remember { mutableStateOf(post.initialLikes) }
    var isLiked by remember { mutableStateOf(false) }

    val heartColor by animateColorAsState(
        targetValue = if (isLiked) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "heartColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9945FF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.avatarEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${post.userHandle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = post.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location & Reward Info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📍", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.locationName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF14F195).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "+${post.rewardSkr} \$SKR",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF14F195),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Like / Boost)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isLiked) {
                        isLiked = false
                        likeCount--
                    } else {
                        isLiked = true
                        likeCount++
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isLiked) "❤️" else "🤍", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$likeCount", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                TextButton(onClick = { /* Boost action */ }) {
                    Text("🚀 Boost (+5 \$SKR)", fontSize = 12.sp)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// MemeFeedCard – картка мему у SocialFi стрічці з кнопкою Tip $SKR
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun MemeFeedCard(meme: MintedMeme) {
    var tipCount by remember { mutableStateOf(0) }
    var tipped    by remember { mutableStateOf(false) }
    var tipError  by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0F2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF9945FF).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "🎨 MemePulse cNFT",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF9945FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = meme.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666688)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meme canvas preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D0D1A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    if (meme.topText.isNotBlank()) {
                        Text(
                            text = meme.topText.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(meme.templateEmoji, fontSize = 52.sp)
                    if (meme.bottomText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = meme.bottomText.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Text(
                    text = "PulseMobile · Solana",
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reward + Tip row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF14F195).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+${meme.skrReward} \$SKR minted",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color(0xFF14F195),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Tip $SKR button
                Button(
                    onClick = {
                        val success = TokenManager.tipMeme(5)
                        if (success) {
                            tipCount++
                            tipped = true
                            tipError = false
                        } else {
                            tipError = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipped) Color(0xFF14F195).copy(alpha = 0.2f) else Color(0xFF9945FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (tipError) "❌ Недостатньо \$SKR" else "💜 Tip \$SKR${if (tipCount > 0) " ($tipCount)" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tipped) Color(0xFF14F195) else Color.White
                    )
                }
            }
        }
    }
}
