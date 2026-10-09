package com.pulse.mobile.data

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class DemoLedgerTest {
    private lateinit var store: MemoryPreferences
    // Synthetic storage fixture, never a configured geographic demo point or device location.
    private fun hotspot() = HotspotConfig.localHotspot(Double.MIN_VALUE, Double.MIN_VALUE)

    @Before fun loadEmptyStore() {
        store = MemoryPreferences()
        TokenManager.restoreLocalState(store)
        MemeRepository.mintedMemes.clear()
    }
    private fun setup() { TokenManager.saveTestHotspot(hotspot()) }
    private fun restart() {
        store = store.reopenFromDisk()
        TokenManager.restoreLocalState(store)
    }
    private fun mustReject(action: () -> Unit) {
        try { action(); fail("Expected operation to be rejected") }
        catch (_: IllegalArgumentException) { }
        catch (_: IllegalStateException) { }
    }

    @Test fun creationSurvivesRestartWithOnlyTwoCenterValues() {
        setup()
        assertEquals(setOf("demo_latitude", "demo_longitude"), store.all.keys)
        restart()
        assertNotNull(TokenManager.demoHotspot)
        assertEquals(100f, TokenManager.demoHotspot!!.radiusMeters, 0f)
        assertEquals(30f, TokenManager.demoHotspot!!.maxAccuracyMeters, 0f)
    }
    @Test fun creationDoesNotAwardPointsAndCannotReplaceExistingCenter() {
        setup()
        mustReject { setup() }
        assertEquals(0, TokenManager.balancePoints)
        assertEquals(0, TokenManager.totalCheckIns)
    }
    @Test fun onlyOneAwardAcrossRepeatedRequests() {
        setup()
        val result = TokenManager.awardVerifiedDemoCheckIn(hotspot())
        mustReject { TokenManager.awardVerifiedDemoCheckIn(hotspot()) }
        assertEquals(result.finalReward, TokenManager.balancePoints)
        assertEquals(25, TokenManager.userXp)
        assertEquals(1, TokenManager.totalCheckIns)
    }
    @Test fun awardReceiptAndBalanceSurviveRestartTogether() {
        setup()
        val expected = TokenManager.awardVerifiedDemoCheckIn(hotspot()).finalReward
        restart()
        assertTrue(TokenManager.demoRewardClaimed)
        assertEquals(expected, TokenManager.balancePoints)
        assertEquals(1, TokenManager.totalCheckIns)
        mustReject { TokenManager.awardVerifiedDemoCheckIn(hotspot()) }
    }
    @Test fun removingCenterErasesCoordinatesButKeepsReceipt() {
        setup()
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        TokenManager.removeTestHotspot()
        assertFalse(store.contains("demo_latitude"))
        assertFalse(store.contains("demo_longitude"))
        restart()
        assertNull(TokenManager.demoHotspot)
        assertTrue(TokenManager.demoRewardClaimed)
        mustReject { setup() }
        mustReject { TokenManager.awardVerifiedDemoCheckIn(hotspot()) }
        assertEquals(1, TokenManager.totalCheckIns)
    }
    @Test fun unclaimedCenterCanBeRemovedWithoutCreatingAReward() {
        setup()
        TokenManager.removeTestHotspot()
        restart()
        assertNull(TokenManager.demoHotspot)
        assertFalse(TokenManager.demoRewardClaimed)
        setup()
        assertEquals(0, TokenManager.balancePoints)
    }
    @Test fun parallelAwardAttemptsCreditExactlyOnce() {
        setup()
        val ready = CountDownLatch(1)
        val successes = AtomicInteger()
        val workers = List(6) {
            Thread {
                ready.await()
                try {
                    TokenManager.awardVerifiedDemoCheckIn(hotspot())
                    successes.incrementAndGet()
                } catch (_: IllegalArgumentException) { }
            }.apply { start() }
        }
        ready.countDown()
        workers.forEach { it.join(5_000); assertFalse(it.isAlive) }
        assertEquals(1, successes.get())
        assertEquals(50, TokenManager.balancePoints)
        assertEquals(1, TokenManager.totalCheckIns)
    }
    @Test fun failedAwardWriteDoesNotCreditAndBlocksAnotherInProcessAttempt() {
        setup()
        store.failNextCommit = true
        mustReject { TokenManager.awardVerifiedDemoCheckIn(hotspot()) }
        assertEquals(0, TokenManager.balancePoints)
        assertEquals(0, TokenManager.totalCheckIns)
        assertTrue(TokenManager.demoRewardSaveUncertain)
        mustReject { TokenManager.awardVerifiedDemoCheckIn(hotspot()) }
        mustReject { TokenManager.claimDailyBonus() }
        restart() // The simulated failed write never reached disk.
        assertFalse(TokenManager.demoRewardClaimed)
        assertFalse(TokenManager.demoRewardSaveUncertain)
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        restart()
        assertEquals(50, TokenManager.balancePoints)
        assertEquals(1, TokenManager.totalCheckIns)
    }
    @Test fun failedCreationWriteDoesNotExposeAHotspotOrAward() {
        store.failNextCommit = true
        mustReject { setup() }
        assertNull(TokenManager.demoHotspot)
        assertEquals(0, TokenManager.balancePoints)
    }
    @Test fun cannotAwardWithoutConfiguredLocalHotspot() {
        mustReject { TokenManager.awardVerifiedDemoCheckIn(hotspot()) }
        assertEquals(0, TokenManager.balancePoints)
    }
    @Test fun dailyBonusIsExactlyTwentyAndRemainsClaimedAfterRestart() {
        assertTrue(TokenManager.claimDailyBonus())
        assertFalse(TokenManager.claimDailyBonus())
        restart()
        assertFalse(TokenManager.claimDailyBonus())
        assertEquals(20, TokenManager.balancePoints)
        assertEquals(0, TokenManager.userXp)
        assertEquals(0, TokenManager.totalCheckIns)
    }
    @Test fun existingLedgerIsPreservedWhenLoadingDemoState() {
        store.edit().putInt("balance", 150).putInt("xp", 120).putInt("check_ins", 5).commit()
        restart()
        setup()
        val result = TokenManager.awardVerifiedDemoCheckIn(hotspot())
        restart()
        assertEquals(150 + result.finalReward, TokenManager.balancePoints)
        assertEquals(145, TokenManager.userXp)
        assertEquals(6, TokenManager.totalCheckIns)
    }
    private fun addMeme() {
        MemeRepository.mintedMemes.add(MintedMeme("fixture", "", "test", "", 50, "just now"))
    }

    @Test fun resetClearsAllDemoEntitiesAndReturnsDocumentedZeroState() {
        setup()
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        TokenManager.claimDailyBonus()
        repeat(8) { TokenManager.publishMemeReward() }
        addMeme()
        assertNotEquals(SeekerTier.BRONZE, TokenManager.currentTier)
        val generation = TokenManager.demoGeneration
        TokenManager.resetDemoData()
        assertTrue(store.all.isEmpty())
        assertEquals(0, TokenManager.balancePoints)
        assertEquals(0, TokenManager.userXp)
        assertEquals(SeekerTier.BRONZE, TokenManager.currentTier)
        assertEquals(0f, TokenManager.xpProgressToNextTier, 0f)
        assertEquals(0, TokenManager.totalCheckIns)
        assertEquals(0, TokenManager.totalMemes)
        assertFalse(TokenManager.dailyClaimed)
        assertFalse(TokenManager.demoRewardClaimed)
        assertFalse(TokenManager.demoRewardSaveUncertain)
        assertNull(TokenManager.demoHotspot)
        assertNull(TokenManager.lastCheckInAtMillis)
        assertNull(TokenManager.lastCheckInPoints)
        assertTrue(MemeRepository.mintedMemes.isEmpty())
        assertEquals(generation + 1, TokenManager.demoGeneration)
        restart()
        assertEquals(0, TokenManager.balancePoints)
        assertEquals(0, TokenManager.userXp)
        assertFalse(TokenManager.dailyClaimed)
        assertFalse(TokenManager.demoRewardClaimed)
        assertNull(TokenManager.demoHotspot)
    }
    @Test fun resetIsExplicitLoadingOrUpdatingStateNeverResetsAClaim() {
        setup()
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        TokenManager.publishMemeReward()
        TokenManager.claimDailyBonus()
        repeat(3) { restart() }
        assertEquals(120, TokenManager.balancePoints)
        assertTrue(TokenManager.demoRewardClaimed)
        assertTrue(TokenManager.dailyClaimed)
        assertEquals(40, TokenManager.userXp)
    }
    @Test fun resetAloneAllowsOneNewMissionAndDailyClaim() {
        setup()
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        TokenManager.claimDailyBonus()
        TokenManager.removeTestHotspot()
        restart()
        mustReject { setup() }
        assertFalse(TokenManager.claimDailyBonus())
        TokenManager.resetDemoData()
        restart()
        setup()
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        mustReject { TokenManager.awardVerifiedDemoCheckIn(hotspot()) }
        assertTrue(TokenManager.claimDailyBonus())
        assertFalse(TokenManager.claimDailyBonus())
        assertEquals(70, TokenManager.balancePoints)
        assertEquals(1, TokenManager.totalCheckIns)
    }
    @Test fun failedResetDoesNotPublishZeroOrClearMemesAndBlocksMutations() {
        setup()
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        addMeme()
        val generation = TokenManager.demoGeneration
        store.failNextCommit = true
        mustReject { TokenManager.resetDemoData() }
        assertEquals(50, TokenManager.balancePoints)
        assertEquals(25, TokenManager.userXp)
        assertTrue(TokenManager.demoRewardClaimed)
        assertTrue(TokenManager.demoRewardSaveUncertain)
        assertNotNull(TokenManager.demoHotspot)
        assertEquals(1, MemeRepository.mintedMemes.size)
        assertEquals(generation, TokenManager.demoGeneration)
        mustReject { TokenManager.publishMemeReward() }
        mustReject { TokenManager.claimDailyBonus() }
        mustReject { TokenManager.tipMeme() }
        mustReject { TokenManager.removeTestHotspot() }
        mustReject { TokenManager.resetDemoData() }
        restart()
        assertEquals(50, TokenManager.balancePoints)
        assertTrue(TokenManager.demoRewardClaimed)
        assertFalse(TokenManager.demoRewardSaveUncertain)
        TokenManager.resetDemoData()
        assertEquals(0, TokenManager.balancePoints)
        assertTrue(MemeRepository.mintedMemes.isEmpty())
    }
    @Test fun repeatedExplicitResetRemainsZeroAndDoesNotTouchSeparateWalletStore() {
        val walletStore = MemoryPreferences()
        walletStore.edit().putString("mwa_authorization", "opaque-test-token").commit()
        TokenManager.resetDemoData()
        TokenManager.resetDemoData()
        restart()
        assertTrue(store.all.isEmpty())
        assertEquals(0, TokenManager.balancePoints)
        assertEquals("opaque-test-token", walletStore.getString("mwa_authorization", null))
    }
    @Test fun preResetStudioRequestCannotCreditNewGeneration() {
        val oldGeneration = TokenManager.demoGeneration
        TokenManager.resetDemoData()
        mustReject { TokenManager.publishMemeReward(oldGeneration) }
        assertEquals(0, TokenManager.balancePoints)
        assertEquals(0, TokenManager.totalMemes)
        assertEquals(50, TokenManager.publishMemeReward(TokenManager.demoGeneration))
    }
    @Test fun receiptAmountAndTimeSurviveRestartAndLocationRemoval() {
        setup()
        val before = System.currentTimeMillis()
        val reward = TokenManager.awardVerifiedDemoCheckIn(hotspot())
        val time = TokenManager.lastCheckInAtMillis!!
        assertTrue(time in before..System.currentTimeMillis())
        TokenManager.removeTestHotspot()
        restart()
        assertEquals(reward.finalReward, TokenManager.lastCheckInPoints)
        assertEquals(time, TokenManager.lastCheckInAtMillis)
        assertTrue(TokenManager.demoRewardClaimed)
        assertNull(TokenManager.demoHotspot)
        mustReject { setup() }
    }
    @Test fun legacyUi03ReceiptRemainsLockedWithoutInventedTimeOrAmount() {
        store.edit().putBoolean("demo_reward_claimed", true).putInt("balance", 50).commit()
        restart()
        assertTrue(TokenManager.demoRewardClaimed)
        assertNull(TokenManager.lastCheckInPoints)
        assertNull(TokenManager.lastCheckInAtMillis)
        mustReject { setup() }
        assertEquals(50, TokenManager.balancePoints)
    }
    @Test fun dailyClaimRespectsUtcBoundaryAndClockRollback() {
        val beforeMidnight = java.time.Instant.parse("2026-10-09T23:59:59Z").toEpochMilli()
        val afterMidnight = beforeMidnight + 1000
        assertTrue(TokenManager.claimDailyBonus(beforeMidnight))
        restart()
        assertFalse(TokenManager.claimDailyBonus(beforeMidnight))
        assertFalse(TokenManager.claimDailyBonus(beforeMidnight - 86400000))
        assertTrue(TokenManager.claimDailyBonus(afterMidnight))
        restart()
        assertFalse(TokenManager.claimDailyBonus(afterMidnight))
        assertEquals(40, TokenManager.balancePoints)
        assertEquals(0, TokenManager.userXp)
        assertEquals(0, TokenManager.totalCheckIns)
    }
    @Test fun studioRewardKeepsOriginalBaseXpCounterAndTierMultiplier() {
        assertEquals(50, TokenManager.publishMemeReward())
        assertEquals(15, TokenManager.userXp)
        assertEquals(1, TokenManager.totalMemes)
        restart()
        assertEquals(50, TokenManager.balancePoints)
        repeat(6) { assertEquals(50, TokenManager.publishMemeReward()) }
        assertEquals(105, TokenManager.userXp)
        assertEquals(SeekerTier.SILVER, TokenManager.currentTier)
        assertEquals(63, TokenManager.publishMemeReward())
        restart()
        assertEquals(413, TokenManager.balancePoints)
        assertEquals(120, TokenManager.userXp)
        assertEquals(8, TokenManager.totalMemes)
        assertEquals(0, TokenManager.totalCheckIns)
    }
    @Test fun failedStudioWriteDoesNotCreditVisibleBalanceXpOrCounter() {
        store.failNextCommit = true
        mustReject { TokenManager.publishMemeReward() }
        assertEquals(0, TokenManager.balancePoints)
        assertEquals(0, TokenManager.userXp)
        assertEquals(0, TokenManager.totalMemes)
        restart()
        assertEquals(0, TokenManager.balancePoints)
    }

    @Test fun resetStorageExceptionFailsClosedUntilRestart() {
        TokenManager.publishMemeReward()
        store.throwNextCommit = true
        mustReject { TokenManager.resetDemoData() }
        assertEquals(50, TokenManager.balancePoints)
        assertTrue(TokenManager.demoRewardSaveUncertain)
        mustReject { TokenManager.publishMemeReward() }
        restart()
        assertEquals(50, TokenManager.balancePoints)
        assertFalse(TokenManager.demoRewardSaveUncertain)
    }

    @Test fun walletRestoreAndDisconnectDoNotChangeAnyDemoData() = kotlinx.coroutines.runBlocking {
        setup()
        TokenManager.awardVerifiedDemoCheckIn(hotspot())
        TokenManager.publishMemeReward()
        TokenManager.claimDailyBonus()
        addMeme()
        val before = store.all.toMap()
        var saved: com.pulse.mobile.mwa.WalletSession? = null
        val walletStore = object : com.pulse.mobile.mwa.WalletSessionStore {
            override fun load() = saved
            override fun save(session: com.pulse.mobile.mwa.WalletSession) { saved = session }
            override fun clear() { saved = null }
        }
        val wallet = object : com.pulse.mobile.mwa.WalletGateway {
            override suspend fun authorize(previous: com.pulse.mobile.mwa.WalletSession?) =
                com.pulse.mobile.mwa.WalletSession("mock-token", "1".repeat(32), null)
            override suspend fun revoke(session: com.pulse.mobile.mwa.WalletSession) { }
        }
        val first = com.pulse.mobile.mwa.WalletSessionController(walletStore, kotlinx.coroutines.Dispatchers.Unconfined)
        first.connect(wallet)
        val cold = com.pulse.mobile.mwa.WalletSessionController(walletStore, kotlinx.coroutines.Dispatchers.Unconfined)
        cold.restoreOnce(wallet)
        cold.disconnect(wallet)
        assertEquals(before, store.all)
        assertEquals(120, TokenManager.balancePoints)
        assertEquals(40, TokenManager.userXp)
        assertEquals(1, MemeRepository.mintedMemes.size)
        assertTrue(TokenManager.demoRewardClaimed)
        assertNotNull(TokenManager.lastCheckInAtMillis)
        assertNotNull(TokenManager.demoHotspot)
    }

}

/** Models both SharedPreferences memory and durable storage, including commit failure. */
private class MemoryPreferences(private var disk: MutableMap<String, Any?> = mutableMapOf()) : SharedPreferences {
    private var memory = disk.toMutableMap()
    var failNextCommit = false
    var throwNextCommit = false
    fun reopenFromDisk() = MemoryPreferences(disk.toMutableMap())
    override fun getAll(): MutableMap<String, *> = memory.toMutableMap()
    override fun contains(key: String?) = memory.containsKey(key)
    override fun getString(key: String?, defValue: String?) = memory[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?) =
        (memory[key] as? Set<String>)?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int) = memory[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long) = memory[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float) = memory[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = memory[key] as? Boolean ?: defValue
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { }
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { }
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clearAll = false
        private fun put(key: String?, value: Any?): SharedPreferences.Editor {
            changes[requireNotNull(key)] = value
            return this
        }
        override fun putString(key: String?, value: String?) = put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toSet())
        override fun putInt(key: String?, value: Int) = put(key, value)
        override fun putLong(key: String?, value: Long) = put(key, value)
        override fun putFloat(key: String?, value: Float) = put(key, value)
        override fun putBoolean(key: String?, value: Boolean) = put(key, value)
        override fun remove(key: String?) = put(key, null)
        override fun clear(): SharedPreferences.Editor { clearAll = true; return this }
        override fun commit(): Boolean {
            if (throwNextCommit) { throwNextCommit = false; error("Simulated storage exception") }
            if (clearAll) memory.clear()
            changes.forEach { (key, value) -> if (value == null) memory.remove(key) else memory[key] = value }
            if (failNextCommit) { failNextCommit = false; return false }
            disk = memory.toMutableMap()
            return true
        }
        override fun apply() { commit() }
    }
}
