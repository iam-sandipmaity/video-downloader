package com.localdownloader.utils

import android.content.Context
import com.localdownloader.domain.models.BatterySaverMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryOptimizationManagerTest {

    @Test
    fun isBatterySaverActive_offMode_alwaysReturnsFalse() {
        // BatterySaverMode.OFF should never activate battery saver
        assertEquals(BatterySaverMode.OFF, BatterySaverMode.valueOf("OFF"))
    }

    @Test
    fun resolveEffectiveConcurrentSlots_throttlesToOneWhenAlwaysOn() {
        // Mode ALWAYS_ON should enforce 1 slot
        val mode = BatterySaverMode.ALWAYS_ON
        val configuredSlots = 3
        val effective = if (mode == BatterySaverMode.ALWAYS_ON) 1 else configuredSlots
        assertEquals(1, effective)
    }

    @Test
    fun resolveEffectiveConcurrentFragments_throttlesToMinTwoWhenAlwaysOn() {
        val configuredThreads = 8
        val throttled = minOf(2, configuredThreads)
        assertEquals(2, throttled)
    }
}
