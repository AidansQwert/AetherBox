package com.droidspaces.app.util

/**
 * Curated HTTPS rootfs.json feeds that users can one-tap subscribe to.
 * All URLs must stay HTTPS (supply-chain rule V13).
 */
data class CuratedRootfsRepo(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val url: String,
    /** Grouping chip in the repo manager: official / lxc / community */
    val category: String = "community"
)

object CuratedRootfsRepos {
    val presets: List<CuratedRootfsRepo> = listOf(
        CuratedRootfsRepo(
            id = "droidspaces-official",
            name = "Droidspaces Official",
            description = "Tuned Ubuntu, Arch, Debian, Alpine, Kali, OpenWrt builds",
            author = "Droidspaces",
            url = "https://raw.githubusercontent.com/Droidspaces/Droidspaces-rootfs-builder/main/rootfs.json",
            category = "official"
        ),
        CuratedRootfsRepo(
            id = "lxc-full-mirror",
            name = "LXC Full Mirror",
            description = "100+ official LXC images (Ubuntu, Arch, Fedora, Mint, …)",
            author = "Droidspaces",
            url = "https://raw.githubusercontent.com/Droidspaces/linuxcontainers-mirror/main/rootfs.json",
            category = "lxc"
        ),
        CuratedRootfsRepo(
            id = "aetherbox-lxc",
            name = "AetherBox LXC Picks",
            description = "Curated Fedora, Void, Rocky, Alma, Gentoo, Devuan, Kali",
            author = "AetherBox",
            url = "https://raw.githubusercontent.com/AidansQwert/AetherBox/main/Android/rootfs-feeds/lxc-community.json",
            category = "lxc"
        ),
        CuratedRootfsRepo(
            id = "steamos-arch",
            name = "SteamOS (Arch base)",
            description = "SteamOS 3 is Arch-based — Arch LXC rootfs for aarch64/x86_64 (not Valve official)",
            author = "AetherBox",
            url = "https://raw.githubusercontent.com/AidansQwert/AetherBox/main/Android/rootfs-feeds/steamos-arch.json",
            category = "community"
        ),
        CuratedRootfsRepo(
            id = "seriatvt-builder",
            name = "seriaTvT Rootfs Builder",
            description = "Community fork of the official Droidspaces rootfs builder",
            author = "seriaTvT",
            url = "https://raw.githubusercontent.com/seriaTvT/Droidspaces-rootfs-builder/main/rootfs.json",
            category = "community"
        ),
        CuratedRootfsRepo(
            id = "shimabde-builder",
            name = "shimabde Rootfs Builder",
            description = "Another community rootfs.json catalog from GitHub",
            author = "shimabde",
            url = "https://raw.githubusercontent.com/shimabde/Droidspaces-rootfs-builder/main/rootfs.json",
            category = "community"
        ),
        CuratedRootfsRepo(
            id = "openwrt-immortal",
            name = "OpenWrt / ImmortalWrt",
            description = "OpenWrt & ImmortalWrt minimal rootfs (zh-cn builds)",
            author = "xys20071111",
            url = "https://raw.githubusercontent.com/xys20071111/Droidspaces-openwrt-rootfs-builder/main/rootfs.json",
            category = "community"
        )
    )

    /** Short GitHub owner/repo hints shown as quick-fill chips. */
    val githubQuickFills: List<Pair<String, String>> = listOf(
        "Droidspaces/Droidspaces-rootfs-builder" to "Official",
        "Droidspaces/linuxcontainers-mirror" to "LXC mirror",
        "AidansQwert/AetherBox@main:Android/rootfs-feeds/steamos-arch.json" to "SteamOS/Arch",
        "seriaTvT/Droidspaces-rootfs-builder" to "seriaTvT",
        "shimabde/Droidspaces-rootfs-builder" to "shimabde",
        "xys20071111/Droidspaces-openwrt-rootfs-builder" to "OpenWrt"
    )

    /**
     * Expand shorthand GitHub paths into a rootfs.json raw URL.
     *
     * Accepted forms:
     * - owner/repo
     * - owner/repo@branch
     * - owner/repo/path/to/rootfs.json
     * - owner/repo@branch:path/to/rootfs.json
     * - full https://… URLs (returned unchanged if already https)
     */
    fun resolveGithubInput(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("https://", ignoreCase = true)) return trimmed
        if (trimmed.startsWith("http://", ignoreCase = true)) return null

        // owner/repo[@branch][:path]  OR  owner/repo/path...
        val atSplit = trimmed.split("@", limit = 2)
        val head = atSplit[0]
        val afterAt = atSplit.getOrNull(1)

        val branch: String
        val path: String
        val ownerRepo: String

        if (afterAt != null) {
            val colon = afterAt.split(":", limit = 2)
            branch = colon[0].ifBlank { "main" }
            path = colon.getOrNull(1)?.trimStart('/')?.ifBlank { null } ?: "rootfs.json"
            ownerRepo = head.trim('/')
        } else {
            val parts = head.trim('/').split('/')
            if (parts.size < 2) return null
            ownerRepo = "${parts[0]}/${parts[1]}"
            branch = "main"
            path = if (parts.size > 2) parts.drop(2).joinToString("/") else "rootfs.json"
        }

        if (!ownerRepo.matches(Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$"))) return null
        return "https://raw.githubusercontent.com/$ownerRepo/$branch/$path"
    }

    /** Suggest a display name from a GitHub owner/repo or URL. */
    fun suggestName(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("https://", ignoreCase = true)) {
            val match = Regex("githubusercontent\\.com/([^/]+)/([^/]+)").find(trimmed)
                ?: Regex("github\\.com/([^/]+)/([^/]+)").find(trimmed)
            if (match != null) return "${match.groupValues[1]}/${match.groupValues[2]}"
            return "Custom feed"
        }
        val parts = trimmed.split("@", limit = 2)[0].trim('/').split('/')
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else trimmed
    }
}
