package com.droidspaces.app.ui.theme

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    COMPACT;

    companion object {
        fun fromName(name: String?): HomeLayout =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: SIMPLE
    }
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
