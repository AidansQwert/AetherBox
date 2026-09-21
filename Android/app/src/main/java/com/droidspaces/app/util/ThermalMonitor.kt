package com.droidspaces.app.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.io.File
import java.util.Locale

/**
 * Host thermal from Android built-in APIs (no root required).
 * Battery °C via sticky battery intent; throttle level via [PowerManager]
 * (API 29+). Optionally peeks world-readable thermal_zone nodes when present.
 */
data class ThermalSnapshot(
    val statusLabel: String,
    val statusLevel: Int,
    val batteryCelsius: Float?,
    val zoneCelsius: Float?,
    val summary: String
)

object ThermalMonitor {
    private const val THERMAL_NONE = 0
    private const val THERMAL_LIGHT = 1
    private const val THERMAL_MODERATE = 2
    private const val THERMAL_SEVERE = 3
    private const val THERMAL_CRITICAL = 4
    private const val THERMAL_EMERGENCY = 5
    private const val THERMAL_SHUTDOWN = 6

    fun snapshot(context: Context): ThermalSnapshot {
        val level = currentThermalLevel(context)
        val statusLabel = statusLabel(level)
        val battery = batteryCelsius(context)
        val zone = zoneCelsius()
        val tempPart = formatTemp(battery) ?: formatTemp(zone)
        val summary = when {
            tempPart != null -> "$statusLabel · $tempPart"
            else -> statusLabel
        }
        return ThermalSnapshot(
            statusLabel = statusLabel,
            statusLevel = level,
            batteryCelsius = battery,
            zoneCelsius = zone,
            summary = summary
        )
    }

    private fun currentThermalLevel(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return THERMAL_NONE
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return THERMAL_NONE
        return try {
            pm.currentThermalStatus
        } catch (_: Exception) {
            THERMAL_NONE
        }
    }

    private fun statusLabel(level: Int): String = when (level) {
        THERMAL_NONE -> "None"
        THERMAL_LIGHT -> "Light"
        THERMAL_MODERATE -> "Moderate"
        THERMAL_SEVERE -> "Severe"
        THERMAL_CRITICAL -> "Critical"
        THERMAL_EMERGENCY -> "Emergency"
        THERMAL_SHUTDOWN -> "Shutdown"
        else -> "Unknown"
    }

    private fun batteryCelsius(context: Context): Float? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val tenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        if (tenths == Int.MIN_VALUE) return null
        return tenths / 10f
    }

    /** Best-effort; many devices hide these without root. */
    private fun zoneCelsius(): Float? {
        val paths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        for (path in paths) {
            try {
                val file = File(path)
                if (!file.canRead()) continue
                val raw = file.readText().trim().toLongOrNull() ?: continue
                if (raw <= 0L) continue
                val c = when {
                    raw > 1000L -> raw / 1000f
                    raw > 200L -> raw / 10f
                    else -> raw.toFloat()
                }
                if (c in 1f..120f) return c
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun formatTemp(celsius: Float?): String? {
        if (celsius == null) return null
        return String.format(Locale.US, "%.1f°C", celsius)
    }
}
