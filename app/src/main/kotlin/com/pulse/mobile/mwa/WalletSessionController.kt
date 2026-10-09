package com.pulse.mobile.mwa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** MWA remains the authority. Cached metadata never produces CONNECTED by itself. */
internal class WalletSessionController(
    private val store: WalletSessionStore, private val storageDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var ui by mutableStateOf(WalletUiState())
        private set
    private var session: WalletSession? = null
    private var pending: WalletSession? = null
    private var startupAttempted = false
    private val gate = AtomicBoolean(false)

    suspend fun restoreOnce(wallet: WalletGateway) {
        if (startupAttempted || !gate.compareAndSet(false, true)) return
        startupAttempted = true
        try {
            session = withContext(storageDispatcher) { store.load() }
            val previous = session
            if (previous == null) { ui = WalletUiState(WalletStatus.DISCONNECTED); return }
            if (previous.token == null) {
                ui = WalletUiState(WalletStatus.EXPIRED, rememberedAddress = previous.address); return
            }
            ui = WalletUiState(WalletStatus.RESTORING, rememberedAddress = previous.address)
            authorize(wallet)
        } catch (e: CancellationException) {
            ui = WalletUiState(WalletStatus.RECONNECT, rememberedAddress = session?.address)
            throw e
        } catch (_: Exception) { storageError() }
        finally { gate.set(false) }
    }
    suspend fun connect(wallet: WalletGateway) {
        if (!gate.compareAndSet(false, true)) return
        try {
            // Retry encrypted I/O first; never fall back to plaintext or silently forget old account.
            session = withContext(storageDispatcher) { store.load() }
            pending = null
            ui = WalletUiState(WalletStatus.CONNECTING, rememberedAddress = session?.address)
            authorize(wallet)
        } catch (e: CancellationException) {
            ui = WalletUiState(WalletStatus.RECONNECT, rememberedAddress = session?.address); throw e
        } catch (_: Exception) { storageError() }
        finally { gate.set(false) }
    }
    private suspend fun authorize(wallet: WalletGateway) {
        val previous = session
        try {
            val fresh = wallet.authorize(previous?.takeIf { it.token != null })
            require(!fresh.token.isNullOrBlank())
            if (previous != null && (previous.address != fresh.address || previous.walletUri != fresh.walletUri)) {
                pending = fresh
                ui = WalletUiState(WalletStatus.ACCOUNT_CHANGED, rememberedAddress = previous.address,
                    candidateAddress = fresh.address, providerChanged = previous.walletUri != fresh.walletUri)
                return
            }
            persistConfirmed(fresh)
        } catch (e: CancellationException) { throw e }
        catch (e: SafeWalletException) {
            if (e.failure == WalletFailure.INVALID_AUTH && previous != null) {
                // Forget the unusable token, keep account binding for explicit reconnect confirmation.
                session = WalletSession(null, previous.address, previous.walletUri)
                withContext(storageDispatcher) { store.save(session!!) }
            }
            ui = WalletUiState(if (e.failure == WalletFailure.INVALID_AUTH) WalletStatus.EXPIRED else WalletStatus.RECONNECT,
                rememberedAddress = session?.address, message = e.message)
        } catch (_: Exception) {
            // Includes save/encryption failure: no misleading CONNECTED or raw exception text.
            storageError()
        }
    }
    private suspend fun persistConfirmed(fresh: WalletSession) {
        withContext(storageDispatcher) { store.save(fresh) }
        session = fresh
        pending = null
        ui = WalletUiState(WalletStatus.CONNECTED, address = fresh.address)
    }
    suspend fun acceptAccountChange() {
        if (!gate.compareAndSet(false, true)) return
        try { pending?.let { persistConfirmed(it) } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { storageError() }
        finally { gate.set(false) }
    }
    fun rejectAccountChange() {
        if (gate.get()) return
        pending = null
        ui = WalletUiState(WalletStatus.RECONNECT, rememberedAddress = session?.address,
            message = "Wallet change was not accepted. Reconnect your previous wallet.")
    }
    suspend fun disconnect(wallet: WalletGateway) {
        if (!gate.compareAndSet(false, true)) return
        val grant = pending ?: session
        try {
            ui = WalletUiState(WalletStatus.DISCONNECTING)
            // Clear local authorization before launching optional wallet-side deauthorization.
            // Finish the short local commit even if Activity recreation cancels wallet work.
            withContext(NonCancellable) {
                withContext(storageDispatcher) { store.clear() }
                session = null; pending = null
                ui = WalletUiState(WalletStatus.DISCONNECTED)
            }
            try {
                grant?.takeIf { it.token != null }?.let { wallet.revoke(it) }
                ui = WalletUiState(WalletStatus.DISCONNECTED, message = "Wallet disconnected.")
            } catch (e: CancellationException) {
                ui = WalletUiState(WalletStatus.DISCONNECTED); throw e
            } catch (_: Exception) {
                ui = WalletUiState(WalletStatus.DISCONNECTED,
                    message = "Disconnected locally. Wallet-side revocation was unavailable; revoke in wallet settings if needed.")
            }
        } catch (e: CancellationException) {
            ui = WalletUiState(if (session == null) WalletStatus.DISCONNECTED else WalletStatus.RECONNECT,
                rememberedAddress = session?.address)
            throw e
        } catch (_: Exception) {
            ui = WalletUiState(WalletStatus.STORAGE_ERROR, rememberedAddress = grant?.address,
                message = "Could not save Disconnect. Retry Disconnect before closing the app.")
        } finally { gate.set(false) }
    }
    private fun storageError() {
        ui = WalletUiState(WalletStatus.STORAGE_ERROR, rememberedAddress = session?.address,
            message = "Encrypted wallet storage unavailable. No wallet session was confirmed. Retry when storage is available.")
    }
}
