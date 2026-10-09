package com.pulse.mobile.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Device-local wall time, English UI text. Never mutates the receipt timestamp. */
internal fun formatReceiptTime(timestampMillis: Long): String =
    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.ENGLISH).format(Date(timestampMillis))
