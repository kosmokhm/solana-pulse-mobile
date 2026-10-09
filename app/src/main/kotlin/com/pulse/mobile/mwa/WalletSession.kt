package com.pulse.mobile.mwa

/** Never use a data class: generated toString/copy must not expose the bearer token. */
internal class WalletSession(val token: String?, val address: String, val walletUri: String?) {
    override fun toString() = "WalletSession(<redacted>)"
}

internal interface WalletSessionStore {
    fun load(): WalletSession?
    fun save(session: WalletSession)
    fun clear()
}

internal interface WalletGateway {
    suspend fun authorize(previous: WalletSession?): WalletSession
    suspend fun revoke(session: WalletSession)
}

enum class WalletStatus { RESTORING, DISCONNECTED, CONNECTING, CONNECTED, RECONNECT, EXPIRED, ACCOUNT_CHANGED, DISCONNECTING, STORAGE_ERROR }

data class WalletUiState(
    val status: WalletStatus = WalletStatus.RESTORING,
    val address: String? = null,
    val rememberedAddress: String? = null,
    val candidateAddress: String? = null,
    val providerChanged: Boolean = false,
    val message: String? = null
) {
    val busy get() = status in setOf(WalletStatus.RESTORING, WalletStatus.CONNECTING, WalletStatus.DISCONNECTING)
    val label get() = when (status) {
        WalletStatus.RESTORING -> "Restoring wallet..."
        WalletStatus.CONNECTED -> "Connected • MWA"
        WalletStatus.EXPIRED -> "Wallet authorization expired"
        WalletStatus.CONNECTING -> "Waiting for authorization"
        WalletStatus.DISCONNECTING -> "Disconnecting wallet..."
        WalletStatus.DISCONNECTED -> "Connect securely with MWA"
        else -> "Reconnect wallet"
    }
}

internal enum class WalletFailure { INVALID_AUTH, UNAVAILABLE, REJECTED, TIMEOUT, INVALID_ACCOUNT, FAILED }

/** Only fixed messages, no SDK exception/cause or response payload escapes into UI/crash text. */
internal class SafeWalletException(val failure: WalletFailure) : Exception(when (failure) {
    WalletFailure.INVALID_AUTH -> "Wallet authorization expired or was revoked. Reconnect wallet."
    WalletFailure.UNAVAILABLE -> "Wallet application unavailable. Install or enable an MWA wallet, then reconnect."
    WalletFailure.REJECTED -> "Wallet authorization rejected or cancelled. Reconnect when ready."
    WalletFailure.TIMEOUT -> "MWA timed out. Reconnect wallet when ready."
    WalletFailure.INVALID_ACCOUNT -> "Wallet returned no valid Solana account."
    WalletFailure.FAILED -> "Could not authorize wallet. Reconnect wallet."
})
