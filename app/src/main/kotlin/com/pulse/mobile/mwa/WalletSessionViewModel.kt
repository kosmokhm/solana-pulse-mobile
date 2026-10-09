package com.pulse.mobile.mwa

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Retains confirmed-in-this-process UI state across Activity recreation; never retains Activity. */
class WalletSessionViewModel internal constructor(private val controller: WalletSessionController) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var gateway: WalletGateway? = null
    private var operation: Job? = null
    val state get() = controller.ui

    internal fun attach(wallet: WalletGateway) {
        gateway = wallet
        operation = scope.launch { controller.restoreOnce(wallet) }
    }
    internal fun detach(wallet: WalletGateway) {
        if (gateway === wallet) { operation?.cancel(); gateway = null }
    }
    fun connect() { gateway?.let { wallet ->
        if (operation?.isActive != true) operation = scope.launch { controller.connect(wallet) }
    } }
    fun disconnect() { gateway?.let { wallet ->
        if (operation?.isActive != true) operation = scope.launch { controller.disconnect(wallet) }
    } }
    fun acceptChange() {
        if (operation?.isActive != true) operation = scope.launch { controller.acceptAccountChange() }
    }
    fun rejectChange() = controller.rejectAccountChange()
    override fun onCleared() { scope.cancel(); gateway = null }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val app = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == WalletSessionViewModel::class.java)
                    return WalletSessionViewModel(WalletSessionController(androidWalletSessionStore(app))) as T
                }
            }
        }
    }
}
