package com.pulse.mobile.mwa

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class WalletSessionTest {
    private val address = "1".repeat(32) // Synthetic public-account fixture, not production wallet data.
    private val otherAddress = "2".repeat(32)
    private fun grant(token: String = "mock-authorization-token", account: String = address,
                      provider: String = "https://mock-wallet.invalid") = WalletSession(token, account, provider)
    private class Store(var value: WalletSession? = null) : WalletSessionStore {
        var failLoad = false
        var failSave = false
        var failClear = false
        override fun load(): WalletSession? { check(!failLoad); return value }
        override fun save(session: WalletSession) { check(!failSave); value = session }
        override fun clear() { check(!failClear); value = null }
    }
    private class Wallet(var answer: WalletSession) : WalletGateway {
        var calls = 0
        var revokes = 0
        var previous: WalletSession? = null
        var error: Exception? = null
        var revokeError = false
        var onAuthorize: suspend () -> Unit = {}
        var onRevoke: suspend () -> Unit = {}
        override suspend fun authorize(previous: WalletSession?): WalletSession {
            calls++; this.previous = previous; onAuthorize(); error?.let { throw it }; return answer
        }
        override suspend fun revoke(session: WalletSession) { revokes++; onRevoke(); check(!revokeError) }
    }
    private fun controller(store: Store) = WalletSessionController(store, Dispatchers.Unconfined)

    @Test fun firstAuthorizationIsSavedBeforeConnected() = runBlocking {
        val store = Store(); val wallet = Wallet(grant()); val c = controller(store)
        c.restoreOnce(wallet)
        assertEquals(WalletStatus.DISCONNECTED, c.ui.status)
        assertEquals(0, wallet.calls)
        c.connect(wallet)
        assertEquals("mock-authorization-token", store.value!!.token)
        assertEquals(address, store.value!!.address)
        assertEquals(WalletStatus.CONNECTED, c.ui.status)
    }
    @Test fun processDeathNewControllerReauthorizesSavedToken() = runBlocking {
        val store = Store(); val first = Wallet(grant()); controller(store).connect(first)
        val newProcess = controller(store); val wallet = Wallet(grant("rotated"))
        assertEquals(WalletStatus.RESTORING, newProcess.ui.status)
        assertNull(newProcess.ui.address)
        wallet.onAuthorize = {
            assertEquals(WalletStatus.RESTORING, newProcess.ui.status)
            assertNull(newProcess.ui.address)
        }
        newProcess.restoreOnce(wallet)
        assertEquals("mock-authorization-token", wallet.previous!!.token)
        assertEquals(WalletStatus.CONNECTED, newProcess.ui.status)
        assertEquals("rotated", store.value!!.token)
    }
    @Test fun coldStartNeverConfirmsOnlyCachedAddress() = runBlocking {
        val store = Store(grant()); val c = controller(store)
        val wallet = Wallet(grant()).apply { error = SafeWalletException(WalletFailure.UNAVAILABLE) }
        c.restoreOnce(wallet)
        assertNull(c.ui.address)
        assertEquals(WalletStatus.RECONNECT, c.ui.status)
        assertEquals(address, c.ui.rememberedAddress)
        assertNotNull(store.value)
    }
    @Test fun rotationOrRepeatedStartupDoesNotReauthorizeAgain() = runBlocking {
        val c = controller(Store(grant()));val wallet = Wallet(grant())
        c.restoreOnce(wallet); c.restoreOnce(wallet); c.restoreOnce(wallet)
        assertEquals(1, wallet.calls)
        assertEquals(WalletStatus.CONNECTED, c.ui.status)
    }
    @Test fun expiredOrRevokedTokenIsRemovedButOldAddressBindingRemains() = runBlocking {
        val store = Store(grant());val c = controller(store)
        val wallet = Wallet(grant()).apply { error = SafeWalletException(WalletFailure.INVALID_AUTH) }
        c.restoreOnce(wallet)
        assertEquals(WalletStatus.EXPIRED, c.ui.status)
        assertNull(store.value!!.token)
        assertEquals(address, store.value!!.address)
        val cold = controller(store); cold.restoreOnce(wallet)
        assertEquals(1, wallet.calls)
        assertEquals(WalletStatus.EXPIRED, cold.ui.status)
        wallet.error = null
        cold.connect(wallet)
        assertNull(wallet.previous)
        assertEquals(WalletStatus.CONNECTED, cold.ui.status)
    }
    @Test fun rejectionDoesNotLoopAndKeepsSavedSession() = runBlocking {
        val store = Store(grant());val c = controller(store)
        val wallet = Wallet(grant()).apply { error = SafeWalletException(WalletFailure.REJECTED) }
        c.restoreOnce(wallet);c.restoreOnce(wallet)
        assertEquals(1, wallet.calls)
        assertEquals(WalletStatus.RECONNECT, c.ui.status)
        assertNotNull(store.value!!.token)
    }
    @Test fun timeoutAllowsOnlyExplicitRetry() = runBlocking {
        val c = controller(Store(grant()));val wallet = Wallet(grant()).apply { error = SafeWalletException(WalletFailure.TIMEOUT) }
        c.restoreOnce(wallet);c.restoreOnce(wallet)
        assertEquals(1, wallet.calls)
        assertTrue(c.ui.message!!.contains("timed out"))
        wallet.error = null;c.connect(wallet)
        assertEquals(2, wallet.calls)
        assertEquals(WalletStatus.CONNECTED, c.ui.status)
    }
    @Test fun explicitDisconnectClearsBeforeRevokeAndDoesNotAutoRestore() = runBlocking {
        val store = Store(grant());val c = controller(store);val wallet = Wallet(grant())
        c.restoreOnce(wallet);c.disconnect(wallet)
        assertNull(store.value);assertEquals(1, wallet.revokes)
        val cold = controller(store);cold.restoreOnce(wallet)
        assertEquals(WalletStatus.DISCONNECTED, cold.ui.status)
        assertEquals(1, wallet.calls)
    }
    @Test fun activityCancellationDuringRevokeCannotResurrectLocalSession() = runBlocking {
        val store = Store(grant()); val c = controller(store); val wallet = Wallet(grant())
        c.restoreOnce(wallet)
        val started = CompletableDeferred<Unit>()
        wallet.onRevoke = { assertNull(store.value); started.complete(Unit); awaitCancellation() }
        val job = launch { c.disconnect(wallet) }; started.await(); job.cancelAndJoin()
        assertEquals(WalletStatus.DISCONNECTED, c.ui.status)
        val cold = controller(store); cold.restoreOnce(wallet)
        assertEquals(WalletStatus.DISCONNECTED, cold.ui.status)
        assertEquals(1, wallet.calls)
    }
    @Test fun unavailableProviderDoesNotPreventLocalDisconnect() = runBlocking {
        val store = Store(grant());val c = controller(store)
        val wallet = Wallet(grant()).apply { error = SafeWalletException(WalletFailure.UNAVAILABLE);revokeError = true }
        c.restoreOnce(wallet);c.disconnect(wallet)
        assertNull(store.value)
        assertEquals(WalletStatus.DISCONNECTED, c.ui.status)
        assertTrue(c.ui.message!!.contains("revocation was unavailable"))
    }
    @Test fun failedClearDoesNotClaimSuccessfulDisconnect() = runBlocking {
        val store = Store(grant());val c = controller(store);val wallet = Wallet(grant())
        c.restoreOnce(wallet);store.failClear = true;c.disconnect(wallet)
        assertEquals(WalletStatus.STORAGE_ERROR, c.ui.status)
        assertTrue(c.ui.message!!.contains("Retry Disconnect"))
        assertNotNull(store.value)
        store.failClear = false;c.disconnect(wallet)
        assertNull(store.value)
    }
    @Test fun changedAccountNeedsExplicitConfirmationAndCancelKeepsPrevious() = runBlocking {
        val store = Store(grant());val c = controller(store);val wallet = Wallet(grant("new", otherAddress))
        c.restoreOnce(wallet)
        assertEquals(WalletStatus.ACCOUNT_CHANGED, c.ui.status)
        assertNull(c.ui.address);assertEquals(otherAddress, c.ui.candidateAddress)
        assertEquals(address, store.value!!.address)
        c.rejectAccountChange()
        assertEquals(address, store.value!!.address)
        assertEquals(WalletStatus.RECONNECT, c.ui.status)
        c.connect(wallet);c.acceptAccountChange()
        assertEquals(otherAddress, store.value!!.address)
        assertEquals("new", store.value!!.token)
        assertEquals(WalletStatus.CONNECTED, c.ui.status)
    }
    @Test fun providerChangeAlsoNeedsConfirmationEvenForSamePublicAddress() = runBlocking {
        val store = Store(grant());val c = controller(store)
        c.restoreOnce(Wallet(grant(provider = "https://other-wallet.invalid")))
        assertTrue(c.ui.providerChanged)
        assertEquals(WalletStatus.ACCOUNT_CHANGED, c.ui.status)
        assertEquals("https://mock-wallet.invalid", store.value!!.walletUri)
    }
    @Test fun encryptedStorageFailureDoesNotLaunchWalletOrShowConnected() = runBlocking {
        val store = Store(grant()).apply { failLoad = true };val c = controller(store);val wallet = Wallet(grant())
        c.restoreOnce(wallet);c.connect(wallet)
        assertEquals(0, wallet.calls)
        assertEquals(WalletStatus.STORAGE_ERROR, c.ui.status)
        assertNull(c.ui.address)
    }
    @Test fun tokenRotationSaveFailureNeverClaimsPersistenceOrConnection() = runBlocking {
        val store = Store(grant()).apply { failSave = true };val c = controller(store)
        c.restoreOnce(Wallet(grant("rotated")))
        assertEquals(WalletStatus.STORAGE_ERROR, c.ui.status)
        assertNull(c.ui.address)
        assertEquals("mock-authorization-token", store.value!!.token)
    }
    @Test fun processTerminationCancellationPreservesStoredGrant() = runBlocking {
        val store = Store(grant());val c = controller(store);val wallet = Wallet(grant())
        val started = CompletableDeferred<Unit>()
        wallet.onAuthorize = { started.complete(Unit); awaitCancellation() }
        val job = launch { c.restoreOnce(wallet) };started.await();job.cancelAndJoin()
        assertEquals("mock-authorization-token", store.value!!.token)
        assertEquals(WalletStatus.RECONNECT, c.ui.status)
        assertNull(c.ui.address)
    }
    @Test fun parallelRequestsShareOneOperationGate() = runBlocking {
        val store = Store(grant());val c = controller(store);val wallet = Wallet(grant())
        val started = CompletableDeferred<Unit>();val finish = CompletableDeferred<Unit>()
        wallet.onAuthorize = { started.complete(Unit); finish.await() }
        val job = launch { c.restoreOnce(wallet) };started.await();c.connect(wallet)
        assertEquals(1, wallet.calls);finish.complete(Unit);job.join()
    }
    @Test fun noTokenInUiExceptionsOrApplicationOutput() = runBlocking {
        val secret = "mock-secret-that-must-never-be-logged"
        val store = Store(grant(secret));val c = controller(store)
        val wallet = Wallet(grant()).apply { error = IllegalStateException(secret) }
        val stdout = System.out;val stderr = System.err;val output = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(output));System.setErr(PrintStream(output))
            c.restoreOnce(wallet)
        } finally { System.setOut(stdout);System.setErr(stderr) }
        assertFalse(c.ui.toString().contains(secret))
        assertFalse(grant(secret).toString().contains(secret))
        assertFalse(SafeWalletException(WalletFailure.FAILED).toString().contains(secret))
        assertFalse(output.toString().contains(secret))
    }
}
