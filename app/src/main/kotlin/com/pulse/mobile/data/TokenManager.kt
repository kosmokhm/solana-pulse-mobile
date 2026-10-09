// Local demo Pulse Points ledger and Seeker tiers; independent of wallet accounts.
package com.pulse.mobile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.runtime.snapshots.Snapshot
import com.pulse.mobile.BuildConfig
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    private lateinit var preferences: SharedPreferences
    var demoHotspot by mutableStateOf<DemoHotspot?>(null)
        private set
    var demoRewardClaimed by mutableStateOf(false)
        private set
    var demoRewardSaveUncertain by mutableStateOf(false)
        private set
    // Invalidates pre-reset asynchronous GPS/publish work; never persisted or wallet-related.
    var demoGeneration by mutableStateOf(0L)
        private set
    var lastCheckInPoints by mutableStateOf<Int?>(null)
        private set
    var lastCheckInAtMillis by mutableStateOf<Long?>(null)
        private set
    private var lastDailyClaim by mutableStateOf("")

    @Synchronized
    fun init(context: Context) {
        if (::preferences.isInitialized) return
        restoreLocalState(context.applicationContext.getSharedPreferences("pulse_points_demo", Context.MODE_PRIVATE))
    }

    /** Also used by persistence tests to simulate a fresh process. */
    @Synchronized
    internal fun restoreLocalState(store: SharedPreferences) {
        preferences = store
        demoRewardSaveUncertain = false
        balancePoints = preferences.getInt("balance", 0)
        userXp = preferences.getInt("xp", 0)
        totalCheckIns = preferences.getInt("check_ins", 0)
        totalMemes = preferences.getInt("memes", 0)
        lastDailyClaim = preferences.getString("daily_claim", "") ?: ""
        demoRewardClaimed = preferences.getBoolean("demo_reward_claimed", false)
        lastCheckInPoints = if (demoRewardClaimed && preferences.contains("receipt_points"))
            preferences.getInt("receipt_points", 0) else null
        lastCheckInAtMillis = if (demoRewardClaimed && preferences.contains("receipt_time"))
            preferences.getLong("receipt_time", 0) else null
        demoHotspot = if (preferences.contains("demo_latitude") && preferences.contains("demo_longitude")) {
            runCatching {
                HotspotConfig.localHotspot(
                    Double.fromBits(preferences.getLong("demo_latitude", 0)),
                    Double.fromBits(preferences.getLong("demo_longitude", 0)))
            }.getOrNull()
        } else null
    }

    private fun today(nowMillis: Long = System.currentTimeMillis()): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(nowMillis))

    val dailyClaimed: Boolean
        get() = lastDailyClaim >= today()

    // Balance and claim date are committed together before publishing UI state.
    @Synchronized
    fun claimDailyBonus(): Boolean = claimDailyBonus(System.currentTimeMillis())

    @Synchronized
    internal fun claimDailyBonus(nowMillis: Long): Boolean {
        check(!demoRewardSaveUncertain) { "Restart the app to verify the demo save first" }
        val date = today(nowMillis)
        if (lastDailyClaim >= date) return false
        check(preferences.edit().putInt("balance", balancePoints + 20)
            .putString("daily_claim", date).commit()) { "Could not save daily bonus" }
        balancePoints += 20
        lastDailyClaim = date
        return true
    }

    private fun persist(balance: Int = balancePoints, xp: Int = userXp,
                        checkIns: Int = totalCheckIns, memes: Int = totalMemes) {
        check(!demoRewardSaveUncertain) { "Restart the app to verify the demo reward save first" }
        check(preferences.edit().putInt("balance", balance).putInt("xp", xp)
            .putInt("check_ins", checkIns).putInt("memes", memes).commit()) {
            "Could not save local Pulse Points"
        }
    }

    var balancePoints by mutableStateOf(0)
        private set

    var userXp by mutableStateOf(0)
        private set

    var totalCheckIns by mutableStateOf(0)
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

    var totalMemes by mutableStateOf(0)
        private set

    // Local demo points only; no minting or transfers.
    @Synchronized
    fun publishMemeReward(expectedGeneration: Long = demoGeneration): Int {
        requireCurrentGeneration(expectedGeneration)
        val baseMemeReward = 50
        val finalReward = calculateReward(baseMemeReward, currentTier, HotspotType.STANDARD)
        persist(balance = balancePoints + finalReward, xp = userXp + 15, memes = totalMemes + 1)
        balancePoints += finalReward
        userXp += 15
        totalMemes += 1
        return finalReward
    }

    // Local demo points only; no minting or transfers.
    @Synchronized
    fun tipMeme(amount: Int = 5): Boolean {
        require(amount > 0)
        return if (balancePoints >= amount) {
            persist(balance = balancePoints - amount)
            balancePoints -= amount
            true
        } else {
            false
        }
    }

    @Synchronized
    fun createTestHotspot(location: Location, expectedGeneration: Long = demoGeneration) {
        requireCurrentGeneration(expectedGeneration)
        val hotspot = HotspotConfig.localHotspot(location.latitude, location.longitude)
        require(hotspot.rejectionReason(location) == null) { "A fresh, precise GPS fix is required" }
        saveTestHotspot(hotspot)
    }

    @Synchronized
    internal fun saveTestHotspot(hotspot: DemoHotspot) {
        check(!demoRewardSaveUncertain) { "Restart the app to verify the demo save first" }
        require(demoHotspot == null) { "A test hotspot already exists" }
        require(!demoRewardClaimed) { "The local demo reward has already been claimed" }
        require(hotspot == HotspotConfig.localHotspot(hotspot.latitude, hotspot.longitude))
        check(preferences.edit().putLong("demo_latitude", hotspot.latitude.toBits())
            .putLong("demo_longitude", hotspot.longitude.toBits()).commit()) {
            "Could not save test hotspot. Please try again."
        }
        demoHotspot = hotspot
    }

    /** Remove location data but retain the reward receipt, preventing setup/reward reset. */
    @Synchronized
    fun removeTestHotspot() {
        check(!demoRewardSaveUncertain) { "Restart the app to verify the demo save first" }
        check(preferences.edit().remove("demo_latitude").remove("demo_longitude").commit()) {
            "Could not remove test hotspot. Please try again."
        }
        demoHotspot = null
    }

    @Synchronized
    fun processCheckIn(hotspot: DemoHotspot, location: Location, expectedGeneration: Long = demoGeneration): RewardResult {
        requireCurrentGeneration(expectedGeneration)
        require(hotspot == demoHotspot) { "Test hotspot has changed; refresh GPS" }
        val rejection = hotspot.rejectionReason(location)
        require(rejection == null) { rejection ?: "GPS verification failed" }
        return awardVerifiedDemoCheckIn(hotspot)
    }

    // Only called by processCheckIn after GPS validation. Internal for ledger tests.
    @Synchronized
    internal fun awardVerifiedDemoCheckIn(hotspot: DemoHotspot): RewardResult {
        check(!demoRewardSaveUncertain) { "Restart the app to verify the demo save first" }
        require(hotspot == demoHotspot && hotspot.id == HotspotConfig.DEMO_ID) { "Test hotspot is unavailable" }
        require(!demoRewardClaimed) { "This demo hotspot reward has already been claimed" }
        val oldTier = currentTier
        val finalReward = calculateReward(hotspot.rewardPoints, oldTier, hotspot.hotspotType)
        // A single durable write binds the receipt to the points and counters.
        // If a write fails, fail closed for this process as SharedPreferences may update memory.
        val claimedAt = System.currentTimeMillis()
        val saved = preferences.edit().putBoolean("demo_reward_claimed", true)
            .putInt("receipt_points", finalReward).putLong("receipt_time", claimedAt)
            .putInt("balance", balancePoints + finalReward).putInt("xp", userXp + 25)
            .putInt("check_ins", totalCheckIns + 1).commit()
        demoRewardClaimed = true
        demoRewardSaveUncertain = !saved
        check(saved) { "Could not confirm the reward save. Restart the app before retrying." }
        lastCheckInPoints = finalReward
        lastCheckInAtMillis = claimedAt
        balancePoints += finalReward
        userXp += 25
        totalCheckIns += 1
        return RewardResult(hotspot.rewardPoints, finalReward, oldTier.multiplier,
            hotspot.hotspotType.multiplier, currentTier != oldTier, currentTier)
    }

    private fun requireCurrentGeneration(expected: Long) {
        check(expected == demoGeneration) { "Demo data was reset. Start a new request." }
    }

    /** Debug-only explicit action. One durable transaction, then one observable snapshot.
     * This preferences file contains only demo data; MWA/wallet storage is not touched.
     */
    @Synchronized
    internal fun resetDemoData() {
        check(BuildConfig.DEBUG) { "Demo reset is only available in debug builds" }
        check(!demoRewardSaveUncertain) { "Restart the app to verify the demo save before resetting" }
        val saved = try { preferences.edit().clear().commit() }
        catch (_: Exception) { false }
        if (!saved) {
            // commit may have changed SharedPreferences memory without reaching disk.
            demoRewardSaveUncertain = true
            error("Reset could not be confirmed. Restart the app to verify local data.")
        }
        Snapshot.withMutableSnapshot {
            balancePoints = 0
            userXp = 0
            totalCheckIns = 0
            totalMemes = 0
            lastDailyClaim = ""
            demoRewardClaimed = false
            demoRewardSaveUncertain = false
            demoHotspot = null
            lastCheckInPoints = null
            lastCheckInAtMillis = null
            MemeRepository.mintedMemes.clear()
            demoGeneration += 1
        }
    }

}
