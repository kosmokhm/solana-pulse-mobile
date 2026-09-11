// TokenManager.kt – $SKR Token Ecosystem & Seeker Tiers Management
package com.pulse.mobile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

enum class SeekerTier(val displayName: String, val multiplier: Float, val requiredXp: Int) {
    BRONZE("Seeker Bronze", 1.0f, 0),
    SILVER("Seeker Silver", 1.25f, 100),
    GOLD("Seeker Gold", 1.5f, 300),
    DIAMOND("Seeker Diamond", 2.0f, 1000);

    companion object {
        fun fromXp(xp: Int): SeekerTier {
            return entries.lastOrNull { xp >= it.requiredXp } ?: BRONZE
        }
    }
}

enum class HotspotType(val displayName: String, val multiplier: Float) {
    STANDARD("Standard Hotspot", 1.0f),
    SPONSORED("Sponsored Hotspot", 1.5f),
    EPIC_EVENT("Epic Event", 2.5f)
}

data class RewardResult(
    val baseReward: Int,
    val finalReward: Int,
    val tierMultiplier: Float,
    val hotspotMultiplier: Float,
    val isTierUpgraded: Boolean,
    val newTier: SeekerTier
)

object TokenManager {
    var balanceSkr by mutableStateOf(150)
        private set

    var userXp by mutableStateOf(120)
        private set

    var totalCheckIns by mutableStateOf(5)
        private set

    val currentTier: SeekerTier
        get() = SeekerTier.fromXp(userXp)

    val nextTier: SeekerTier?
        get() {
            val tiers = SeekerTier.entries
            val currentIndex = tiers.indexOf(currentTier)
            return if (currentIndex < tiers.size - 1) tiers[currentIndex + 1] else null
        }

    val xpProgressToNextTier: Float
        get() {
            val next = nextTier ?: return 1.0f
            val currentBase = currentTier.requiredXp
            val needed = next.requiredXp - currentBase
            val gained = userXp - currentBase
            return (gained.toFloat() / needed.toFloat()).coerceIn(0.0f, 1.0f)
        }

    fun calculateReward(baseReward: Int, tier: SeekerTier, hotspotType: HotspotType): Int {
        val totalMultiplier = tier.multiplier * hotspotType.multiplier
        return (baseReward * totalMultiplier).roundToInt()
    }

    fun processCheckIn(baseReward: Int, hotspotType: HotspotType): RewardResult {
        val oldTier = currentTier
        val finalReward = calculateReward(baseReward, oldTier, hotspotType)

        balanceSkr += finalReward
        userXp += 25
        totalCheckIns += 1

        val newTier = currentTier
        val upgraded = newTier != oldTier

        return RewardResult(
            baseReward = baseReward,
            finalReward = finalReward,
            tierMultiplier = oldTier.multiplier,
            hotspotMultiplier = hotspotType.multiplier,
            isTierUpgraded = upgraded,
            newTier = newTier
        )
    }
}
