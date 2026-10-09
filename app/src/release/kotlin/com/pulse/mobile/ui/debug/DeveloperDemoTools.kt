package com.pulse.mobile.ui.debug

import androidx.compose.runtime.Composable

/** Release builds contain no demo-tools entry point or reset UI. */
@Composable
fun DeveloperDemoTools(onReset: () -> Unit) = Unit
