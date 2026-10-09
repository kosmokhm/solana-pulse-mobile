package com.pulse.mobile.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.mobile.BuildConfig
import com.pulse.mobile.data.TokenManager
import com.pulse.mobile.ui.*

/** Included only by the debug source set; opening this dialog never resets data. */
@Composable
fun DeveloperDemoTools(onReset: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        PulseIcon(PulseGlyph.Shield, description = "Developer Demo Tools", color = PulseMuted)
    }
    if (open) AlertDialog(onDismissRequest = { if (!resetting) { open = false; confirming = false } },
        title = { Text(if (confirming) "Reset all local demo data?" else "Developer Demo Tools") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PulseBadge("DEBUG ONLY / ${BuildConfig.VERSION_NAME}")
                Text(if (confirming)
                    "This removes local Pulse Points, XP, daily claim, receipts, test hotspot and demo memes. The next state is 0 points, 0 XP, Bronze, with no claims or test zone."
                    else "Reset starts a fresh test mission. It is never automatic and does not disconnect your wallet.")
                Text("Your real wallet, MWA authorization and other apps are untouched. No transactions are created.", color = PulseMuted)
                message?.let { PulseNotice(it, error) }
            }
        }, confirmButton = {
            TextButton(enabled = !resetting && !TokenManager.demoRewardSaveUncertain, onClick = {
                if (!confirming) { confirming = true; message = null }
                else {
                    resetting = true
                    try {
                        TokenManager.resetDemoData()
                        onReset()
                        error = false
                        message = "Reset complete: 0 points, 0 XP. Your wallet stays connected."
                    } catch (e: Exception) {
                        error = true
                        message = e.message ?: "Reset failed. Restart to verify local data."
                    } finally { resetting = false; confirming = false }
                }
            }) { Text(if (resetting) "Resetting…" else if (confirming) "Confirm Reset" else "Reset Demo Data") }
        }, dismissButton = {
            TextButton(enabled = !resetting, onClick = {
                if (confirming) confirming = false else open = false
            }) { Text(if (confirming) "Cancel" else "Close") }
        })
}
