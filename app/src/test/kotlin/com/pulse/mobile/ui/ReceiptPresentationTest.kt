package com.pulse.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ReceiptPresentationTest {
    private fun withDefaults(locale: Locale, zone: String, block: () -> Unit) {
        val oldLocale = Locale.getDefault()
        val oldZone = TimeZone.getDefault()
        try {
            Locale.setDefault(locale)
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            block()
        } finally {
            Locale.setDefault(oldLocale)
            TimeZone.setDefault(oldZone)
        }
    }
    private val receipt = java.time.Instant.parse("2026-10-09T12:23:30Z").toEpochMilli()

    @Test fun englishDateDespiteUkrainianSystemLocale() {
        withDefaults(Locale.forLanguageTag("uk-UA"), "UTC") {
            assertEquals("9 October 2026, 12:23", formatReceiptTime(receipt))
        }
    }
    @Test fun keepsDeviceLocalTimeAndYearWithoutChangingTimestamp() {
        withDefaults(Locale.forLanguageTag("uk-UA"), "GMT+03:00") {
            assertEquals("9 October 2026, 15:23", formatReceiptTime(receipt))
        }
    }
    @Test fun englishTextAndDigitsDespiteArabicSystemLocale() {
        withDefaults(Locale.forLanguageTag("ar-EG"), "UTC") {
            assertEquals("1 January 1970, 00:00", formatReceiptTime(0L))
        }
    }
}
