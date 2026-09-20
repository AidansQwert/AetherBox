package com.droidspaces.app.util

import android.content.Context
import com.droidspaces.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class RootfsAsset(
    val name: String,
    val description: String,
    val architecture: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val buildDate: String,
    val author: String,
    val sourceRepoName: String,
    val sha256: String = "",
    val version: String = "",
    val distro: String = ""
) {
    // Unique filename derived from metadata - avoids conflicts with generic names like rootfs.tar.xz
    // Includes a short hash of the download URL to guarantee uniqueness even across repos
    val uniqueFilename: String get() {
        val ext = when {
            downloadUrl.endsWith(".tar.xz") -> ".tar.xz"
            downloadUrl.endsWith(".tar.gz")  -> ".tar.gz"
            else -> ".tar.xz"
        }
        val sanitized = "$name $author".trim()
            .replace(Regex("[^a-zA-Z0-9._-]"), "-")
            .replace(Regex("-{2,}"), "-")
            .trimEnd('-')
        val urlHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(downloadUrl.toByteArray())
            .take(4)
            .joinToString("") { "%02x".format(it) }
        return "$sanitized-$architecture-$buildDate-$urlHash$ext"
    }

    val displayDistro: String
        get() = distro.ifBlank {
            name.substringBefore(" - ").substringBefore(" GNU").trim().ifBlank { name }
        }
}

sealed class RepoResult {
    data class Success(val assets: List<RootfsAsset>) : RepoResult()
    data class Error(val message: String) : RepoResult()
}

object RootfsRepository {

    private const val OFFICIAL_REPO_URL =
        "https://github.com/Droidspaces/Droidspaces-rootfs-builder/raw/refs/heads/main/rootfs.json"
    private const val OFFICIAL_REPO_NAME = "Droidspaces Official"
    private const val COMMUNITY_ASSET = "rootfs_lxc_community.json"
    private const val COMMUNITY_REPO_NAME = "LXC Community"
    private const val COMMUNITY_REMOTE_URL =
        "https://raw.githubusercontent.com/Rezalgabteng/Droidspaces-OSS/main/Android/rootfs-feeds/lxc-community.json"
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT    = 15_000

    suspend fun fetchAllAssets(context: Context): RepoResult = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager.getInstance(context)
        val customRepos = prefs.getCustomRepos()

        val fetches = buildList {
            add(async { fetchSingleRepo(OFFICIAL_REPO_URL, OFFICIAL_REPO_NAME) })
            if (prefs.includeCommunityRepos) {
                add(async { fetchCommunityRepo(context) })
            }
            customRepos.forEach { (name, url) ->
                add(async { fetchSingleRepo(url, name) })
            }
        }

        val results = fetches.awaitAll()

        val allAssets = mutableListOf<RootfsAsset>()
        val errors = mutableListOf<String>()

        results.forEach { result ->
            when (result) {
                is RepoResult.Success -> allAssets.addAll(result.assets)
                is RepoResult.Error   -> errors.add(result.message)
            }
        }

        // Prefer the first occurrence of a download URL (official wins over community).
        val deduped = allAssets.distinctBy { it.downloadUrl }

        val arch = DeviceArch.suffix(context)
        val filtered = deduped.filter { archMatches(it.architecture, arch) }
            .sortedWith(
                compareBy<RootfsAsset> { it.displayDistro.lowercase() }
                    .thenBy { it.name.lowercase() }
            )

        return@withContext when {
            filtered.isNotEmpty() -> RepoResult.Success(filtered)
            errors.isNotEmpty()    -> RepoResult.Error(errors.joinToString("\n"))
            else                   -> RepoResult.Error(context.getString(R.string.repo_error_no_assets))
        }
    }

    private fun archMatches(assetArch: String, deviceArch: String): Boolean {
        if (assetArch.equals(deviceArch, ignoreCase = true)) return true
        // LXC feeds use arm64 / amd64 / armhf / i386 naming.
        return when (deviceArch) {
            "aarch64" -> assetArch.equals("arm64", ignoreCase = true)
            "x86_64"  -> assetArch.equals("amd64", ignoreCase = true) ||
                assetArch.equals("x86_64", ignoreCase = true)
            "x86"     -> assetArch.equals("i386", ignoreCase = true) ||
                assetArch.equals("i686", ignoreCase = true)
            "armhf"   -> assetArch.equals("armhf", ignoreCase = true) ||
                assetArch.equals("armv7l", ignoreCase = true)
            else -> false
        }
    }

    private fun fetchCommunityRepo(context: Context): RepoResult {
        // Prefer a fresh remote catalog when available; fall back to the APK-bundled snapshot.
        val remote = fetchSingleRepo(COMMUNITY_REMOTE_URL, COMMUNITY_REPO_NAME)
        if (remote is RepoResult.Success) return remote

        return runCatching {
            val json = context.assets.open(COMMUNITY_ASSET).bufferedReader().use { it.readText() }
            val assets = parseRootfsJson(json, COMMUNITY_REPO_NAME)
            if (assets.isEmpty()) RepoResult.Error("$COMMUNITY_REPO_NAME: no assets found")
            else RepoResult.Success(assets)
        }.getOrElse { e ->
            when (remote) {
                is RepoResult.Error -> remote
                else -> RepoResult.Error("$COMMUNITY_REPO_NAME: ${e.message ?: "unknown error"}")
            }
        }
    }

    private fun fetchSingleRepo(url: String, repoName: String): RepoResult {
        return runCatching {
            val json = httpGet(url)
                ?: return RepoResult.Error("$repoName: network error")
            val assets = parseRootfsJson(json, repoName)
            if (assets.isEmpty()) RepoResult.Error("$repoName: no assets found")
            else RepoResult.Success(assets)
        }.getOrElse { e ->
            RepoResult.Error("$repoName: ${e.message ?: "unknown error"}")
        }
    }

    internal fun httpGet(url: String): String? {
        // Refuse cleartext: the rootfs supply chain must not be MITM-able (V13).
        if (!url.startsWith("https://", ignoreCase = true)) return null
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout    = READ_TIMEOUT
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json,application/vnd.github+json,*/*")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            return null
        }
        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        return body
    }

    internal fun parseRootfsJson(json: String, repoName: String): List<RootfsAsset> {
        val arr = JSONArray(json)
        return buildList {
            for (i in 0 until arr.length()) {
                val obj         = arr.getJSONObject(i)
                val downloadUrl = obj.optString("download_url", "")
                if (downloadUrl.isBlank()) continue
                add(
                    RootfsAsset(
                        name           = obj.optString("name", ""),
                        description    = obj.optString("description", ""),
                        architecture   = obj.optString("architecture", ""),
                        downloadUrl    = downloadUrl,
                        sizeBytes      = obj.optLong("size_bytes", 0L),
                        buildDate      = obj.optString("build_date", ""),
                        author         = obj.optString("author", repoName),
                        sourceRepoName = repoName,
                        sha256         = obj.optString("sha256", ""),
                        version        = obj.optString("version", ""),
                        distro         = obj.optString("distro", "")
                    )
                )
            }
        }
    }
}
