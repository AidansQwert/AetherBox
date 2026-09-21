package com.droidspaces.app.ui.theme

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidspaces.app.util.Constants
import com.droidspaces.app.util.PreferencesManager

/**
 * Home screen layout variants the user can switch between in Appearance.
 */
enum class HomeLayout {
    /** Calm, airy stack — default after the elegant redesign. */
    SIMPLE,
    /** Hero brand plane + quick orbit shortcuts. */
    COMMAND,
    /** Dense list-style metrics and tighter spacing. */
    COMPACT,
    /** Card grid for rootfs + metrics — strong on tablets. */
    GALLERY,
    /** Essentials only: status, metrics, one quick jump. */
    FOCUS,
    /** Two-column home when width allows. */
    DASHBOARD;

    companion object {
        fun fromName(name: String?): HomeLayout =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SIMPLE
    }

    fun sectionGap(): Dp = when (this) {
        COMPACT, FOCUS -> 12.dp
        COMMAND, GALLERY -> 16.dp
        SIMPLE -> 20.dp
        DASHBOARD -> 18.dp
    }

    fun horizontalPad(): Dp = when (this) {
        COMMAND -> 16.dp
        COMPACT -> 16.dp
        else -> 20.dp
    }

    fun isCompactMetrics(): Boolean = this == COMPACT || this == FOCUS

    fun showOrbit(): Boolean = this == COMMAND || this == DASHBOARD

    fun showAppearanceExpanded(): Boolean = this != FOCUS
}

@Composable
fun rememberHomeLayout(): HomeLayout {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    var layout by remember { mutableStateOf(HomeLayout.fromName(prefsManager.homeLayout)) }

    DisposableEffect(prefsManager) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Constants.KEY_HOME_LAYOUT) {
                layout = HomeLayout.fromName(prefsManager.homeLayout)
            }
        }
        prefsManager.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefsManager.prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return layout
}
