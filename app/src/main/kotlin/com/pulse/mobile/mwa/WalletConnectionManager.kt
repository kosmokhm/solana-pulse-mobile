package com.pulse.mobile.mwa

import android.net.Uri
import androidx.activity.ComponentActivity
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.protocol.JsonRpc20Client
import com.solana.mobilewalletadapter.common.ProtocolContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext
import java.math.BigInteger
import java.util.concurrent.TimeoutException

/** SDK 2.2.0 authorization/deauthorization only; no signing or financial operations. */
internal class WalletConnectionManager(activity: ComponentActivity) : WalletGateway {
    private val sender = ActivityResultSender(activity)
    private val adapter = MobileWalletAdapter(ConnectionIdentity(
        identityUri = Uri.parse("https://github.com/kosmokhm/solana-pulse-mobile"),
        iconUri = Uri.parse("/favicon.ico"), identityName = "PulseMobile Demo"
    ))

    override suspend fun authorize(previous: WalletSession?): WalletSession {
        adapter.authToken = previous?.token
        val result = bounded { adapter.connect(sender) }
        coroutineContext.ensureActive()
        return when (result) {
            is TransactionResult.Success -> {
                val auth = result.authResult
                val account = auth.accounts.firstOrNull { account ->
                    previous != null && account.publicKey.size == 32 && base58(account.publicKey) == previous.address
                } ?: auth.accounts.firstOrNull()
                val key = account?.publicKey
                if (key == null || key.size != 32 || auth.authToken.isBlank())
                    throw SafeWalletException(WalletFailure.INVALID_ACCOUNT)
                WalletSession(auth.authToken, base58(key), auth.walletUriBase?.toString())
            }
            is TransactionResult.NoWalletFound -> throw SafeWalletException(WalletFailure.UNAVAILABLE)
            is TransactionResult.Failure -> throw classify(result.e)
        }
    }
    override suspend fun revoke(session: WalletSession) {
        adapter.authToken = session.token
        try {
            val result = bounded { adapter.disconnect(sender) }
            coroutineContext.ensureActive()
            when (result) {
                is TransactionResult.Success -> Unit
                is TransactionResult.NoWalletFound -> throw SafeWalletException(WalletFailure.UNAVAILABLE)
                is TransactionResult.Failure -> throw classify(result.e)
            }
        } finally { adapter.authToken = null }
    }
    private suspend fun <T> bounded(block: suspend () -> T): T = try {
        withTimeout(60_000) { block() }
    } catch (_: TimeoutCancellationException) { throw SafeWalletException(WalletFailure.TIMEOUT) }
    catch (e: CancellationException) { throw e }
    catch (_: Exception) { throw SafeWalletException(WalletFailure.FAILED) }

    private fun classify(error: Throwable): SafeWalletException {
        val causes = generateSequence(error) { it.cause }.take(12).toList()
        val failure = when {
            causes.any { it is JsonRpc20Client.JsonRpc20RemoteException && it.code == ProtocolContract.ERROR_AUTHORIZATION_FAILED } -> WalletFailure.INVALID_AUTH
            causes.any { it is TimeoutException || it is TimeoutCancellationException } -> WalletFailure.TIMEOUT
            causes.any { it is InterruptedException || it is CancellationException } -> WalletFailure.REJECTED
            else -> WalletFailure.FAILED
        }
        return SafeWalletException(failure)
    }
    private fun base58(bytes: ByteArray): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        var value = BigInteger(1, bytes)
        val radix = BigInteger.valueOf(58)
        val encoded = StringBuilder()
        while (value.signum() > 0) {
            val parts = value.divideAndRemainder(radix)
            encoded.append(alphabet[parts[1].toInt()])
            value = parts[0]
        }
        repeat(bytes.takeWhile { it == 0.toByte() }.size) { encoded.append('1') }
        return encoded.reverse().toString()
    }
}
