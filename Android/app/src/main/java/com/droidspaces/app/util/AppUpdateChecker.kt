package com.droidspaces.app.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppUpdateInfo(
    val version: String,
    val displayVersion: String,
    val releaseUrl: String,
    val apkUrl: String?
)

object AppUpdateChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/AidansQwert/AetherBox/releases/latest"

    // Blocking, call from Dispatchers.IO. Null when the installed build is
    // already current, the tag was dismissed, or on any failure.
    fun fetchLatest(context: Context): AppUpdateInfo? = runCatching {
        val prefs = PreferencesManager.getInstance(context)
        val body = RootfsRepository.httpGet(LATEST_RELEASE_URL) ?: return null
        val json = JSONObject(body)
        val tag = json.optString("tag_name")
        val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val latest = versionNumber(tag) ?: return null
        val current = versionNumber(installed ?: return null) ?: return null
        if (latest <= current) return null
        val dismissed = prefs.dismissedAppUpdateTag
        if (dismissed.isNotBlank() && versionNumber(dismissed)?.let { it >= latest } == true) {
            return null
        }
        val apkUrl = pickApkAssetUrl(json.optJSONArray("assets"))
        val display = versionNumber(tag)?.let {
            Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(tag)?.value
        } ?: tag
        AppUpdateInfo(
            version = tag,
            displayVersion = display ?: tag,
            releaseUrl = json.optString("html_url"),
            apkUrl = apkUrl
        )
    }.getOrNull()

    private fun pickApkAssetUrl(assets: JSONArray?): String? {
        if (assets == null) return null
        var fallback: String? = null
        for (i in 0 until assets.length()) {
            val obj = assets.optJSONObject(i) ?: continue
            val name = obj.optString("name", "")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            val url = obj.optString("browser_download_url").ifBlank {
                obj.optString("url")
            }
            if (url.isBlank()) continue
            if (name.contains("universal", ignoreCase = true) ||
                name.contains("AetherBox", ignoreCase = true)
            ) {
                return url
            }
            if (fallback == null) fallback = url
        }
        return fallback
    }

    // Release tags are vX.Y.Z and the installed versionName is X.Y.Z.
    fun versionNumber(text: String): Int? =
        Regex("(\\d+)\\.(\\d+)\\.(\\d+)").find(text)?.let { m ->
            val (major, minor, patch) = m.destructured
            major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt()
        }
}
