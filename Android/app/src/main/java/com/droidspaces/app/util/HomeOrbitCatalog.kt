package com.droidspaces.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.droidspaces.app.R

enum class HomeOrbitActionId {
    SPACES,
    LIVE,
    IMAGES,
    RESUME,
    SETTINGS;

    companion object {
        val DEFAULT_SLOTS = listOf(SPACES, LIVE, IMAGES)

        fun fromName(name: String?): HomeOrbitActionId? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }

        fun parseSlots(raw: String?): List<HomeOrbitActionId> {
            val parsed = raw.orEmpty()
                .split(',')
                .mapNotNull { fromName(it.trim()) }
                .distinct()
            return if (parsed.size >= 3) parsed.take(3) else DEFAULT_SLOTS
        }
    }
}

data class HomeOrbitActionDef(
    val id: HomeOrbitActionId,
    val labelRes: Int,
    val icon: ImageVector
)

object HomeOrbitCatalog {
    val all: List<HomeOrbitActionDef> = listOf(
        HomeOrbitActionDef(HomeOrbitActionId.SPACES, R.string.home_orbit_spaces, Icons.Default.Layers),
        HomeOrbitActionDef(HomeOrbitActionId.LIVE, R.string.home_orbit_live, Icons.Default.RocketLaunch),
        HomeOrbitActionDef(HomeOrbitActionId.IMAGES, R.string.home_orbit_images, Icons.Default.CloudDownload),
        HomeOrbitActionDef(HomeOrbitActionId.RESUME, R.string.home_orbit_resume, Icons.Default.PlayCircle),
        HomeOrbitActionDef(HomeOrbitActionId.SETTINGS, R.string.home_orbit_settings, Icons.Default.Settings)
    )

    fun def(id: HomeOrbitActionId): HomeOrbitActionDef =
        all.first { it.id == id }
}
