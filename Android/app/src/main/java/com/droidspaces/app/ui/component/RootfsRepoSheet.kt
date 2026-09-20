package com.droidspaces.app.ui.component

import android.net.Uri

import androidx.compose.animation.animateColorAsState
import com.droidspaces.app.util.CuratedRootfsRepos
import com.droidspaces.app.util.CuratedRootfsRepo
import com.droidspaces.app.util.AnimationUtils
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import com.droidspaces.app.ui.theme.SheetShape
import com.droidspaces.app.ui.theme.CardShape
import com.droidspaces.app.ui.theme.PillShape
import com.droidspaces.app.ui.theme.ActionButtonShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidspaces.app.ui.component.DsDialog
import com.droidspaces.app.R
import com.droidspaces.app.ui.util.ClearFocusOnClickOutside
import com.droidspaces.app.ui.util.FocusUtils
import com.droidspaces.app.ui.util.FullScreenLoading
import com.droidspaces.app.ui.viewmodel.AssetDownloadState
import com.droidspaces.app.ui.viewmodel.RepoUiState
import com.droidspaces.app.ui.viewmodel.RootfsRepoViewModel
import com.droidspaces.app.util.IconUtils
import com.droidspaces.app.util.RootfsAsset
import com.droidspaces.app.util.PreferencesManager
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootfsRepoSheet(
    onDismiss: () -> Unit,
    onInstall: (Uri) -> Unit
) {
    val vm: RootfsRepoViewModel = viewModel()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showRepoManager by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { sheetState.currentValue }
            .first { it == SheetValue.Expanded }
        vm.load()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        ClearFocusOnClickOutside(modifier = Modifier.fillMaxWidth()) {
        // fillMaxSize, not fillMaxWidth: the sheet skips its partially-expanded
        // state, so it is full height. Without it the column wraps its content and
        // every state below sits stacked at the top of a tall, mostly empty sheet.
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            var searchQuery by remember { mutableStateOf("") }
            var distroFilter by remember { mutableStateOf<String?>(null) }
            var favoritesOnly by remember { mutableStateOf(false) }
            var favoriteTick by remember { mutableStateOf(0) }

            // Derive stable display state - avoids AnimatedContent recomposition that collapses the sheet
            val state = vm.uiState
            val isLoading = state is RepoUiState.Loading || state is RepoUiState.Idle
            val displayAssets = when (state) {
                is RepoUiState.Success -> state.assets
                is RepoUiState.Loading -> state.previousAssets
                else -> emptyList()
            }
            val showError = state is RepoUiState.Error
            val favoriteUrls = remember(favoriteTick, displayAssets) { vm.favoriteUrls() }

            val distroOptions = remember(displayAssets) {
                displayAssets.map { it.displayDistro }.distinct().sorted()
            }

            val filteredAssets = remember(displayAssets, searchQuery, distroFilter, favoritesOnly, favoriteUrls) {
                displayAssets.asSequence()
                    .filter { asset ->
                        if (favoritesOnly && asset.downloadUrl !in favoriteUrls) return@filter false
                        if (distroFilter != null && !asset.displayDistro.equals(distroFilter, ignoreCase = true)) {
                            return@filter false
                        }
                        if (searchQuery.isBlank()) return@filter true
                        asset.name.contains(searchQuery, ignoreCase = true) ||
                            asset.description.contains(searchQuery, ignoreCase = true) ||
                            asset.author.contains(searchQuery, ignoreCase = true) ||
                            asset.sourceRepoName.contains(searchQuery, ignoreCase = true) ||
                            asset.displayDistro.contains(searchQuery, ignoreCase = true) ||
                            asset.version.contains(searchQuery, ignoreCase = true)
                    }
                    .sortedByDescending { it.downloadUrl in favoriteUrls }
                    .toList()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.repo_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = context.getString(R.string.repo_catalog_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
                // Manage / add repos
                IconButton(onClick = { showRepoManager = true }) {
                    Icon(Icons.Default.Tune, contentDescription = context.getString(R.string.repo_manage_custom))
                }
                // Refresh
                IconButton(onClick = { vm.load() }, enabled = !isLoading) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = context.getString(R.string.repo_refresh)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp
            )

            if (displayAssets.isNotEmpty()) {
                RepoSearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                DistroFilterRow(
                    distros = distroOptions,
                    selected = distroFilter,
                    favoritesOnly = favoritesOnly,
                    onSelectDistro = { distroFilter = it },
                    onToggleFavorites = { favoritesOnly = !favoritesOnly }
                )
            }

            // Content: list stays in composition during refresh so sheet height is stable.
            // Takes the height the header and the bottom spacer leave behind, so the
            // loading and error states centre in the real empty area.
            Box(modifier = Modifier.weight(1f)) {
            when {
                displayAssets.isNotEmpty() -> {
                    Box {
                        if (filteredAssets.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        text = context.getString(R.string.repo_not_found_in_repo, searchQuery.ifBlank { distroFilter ?: "…" }),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            RepoListContent(
                                assets         = filteredAssets,
                                isFiltered     = searchQuery.isNotBlank() || distroFilter != null || favoritesOnly,
                                downloadStates = vm.downloadStates,
                                favoriteUrls   = favoriteUrls,
                                onDownload     = { vm.startDownload(it) },
                                onCancel       = { vm.cancelDownload(it) },
                                onInstall      = { uri -> onInstall(uri) },
                                onRetry        = { vm.resetAsset(it.downloadUrl) },
                                onToggleFavorite = {
                                    vm.toggleFavorite(it)
                                    favoriteTick++
                                }
                            )
                        }
                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                            )
                        }
                    }
                }

                showError -> RepoErrorContent(
                    message = (state as RepoUiState.Error).message,
                    onRetry = { vm.load() }
                )

                else -> RepoLoadingContent()
            }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
        } // ClearFocusOnClickOutside
    }

    if (showRepoManager) {
        RepoManagerDialog(
            initialRepos = vm.getCustomRepos(),
            includeCommunity = vm.includeCommunityRepos,
            onCommunityChange = { vm.includeCommunityRepos = it },
            onDismiss    = { showRepoManager = false },
            onSave       = { toAdd, toRemove ->
                toRemove.forEach { vm.removeCustomRepo(it) }
                toAdd.forEach { (name, url) -> vm.addCustomRepo(name, url) }
                showRepoManager = false
            }
        )
    }
}

@Composable
private fun RepoLoadingContent() {
    val context = LocalContext.current
    FullScreenLoading(message = context.getString(R.string.fetching_distros))
}

@Composable
private fun RepoErrorContent(message: String, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = context.getString(R.string.repo_failed_to_load),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(context.getString(R.string.repo_retry))
        }
    }
}

@Composable
private fun DistroFilterRow(
    distros: List<String>,
    selected: String?,
    favoritesOnly: Boolean,
    onSelectDistro: (String?) -> Unit,
    onToggleFavorites: () -> Unit
) {
    val context = LocalContext.current
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null && !favoritesOnly,
                onClick = {
                    onSelectDistro(null)
                    if (favoritesOnly) onToggleFavorites()
                },
                label = { Text(context.getString(R.string.repo_filter_all)) },
                shape = PillShape,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == null && !favoritesOnly,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            )
        }
        item {
            FilterChip(
                selected = favoritesOnly,
                onClick = onToggleFavorites,
                label = { Text(context.getString(R.string.repo_filter_favorites)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (favoritesOnly) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = PillShape,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = favoritesOnly,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            )
        }
        items(distros, key = { it }) { distro ->
            FilterChip(
                selected = selected.equals(distro, ignoreCase = true) && !favoritesOnly,
                onClick = {
                    onSelectDistro(if (selected.equals(distro, ignoreCase = true)) null else distro)
                    if (favoritesOnly) onToggleFavorites()
                },
                label = { Text(distro) },
                shape = PillShape,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected.equals(distro, ignoreCase = true) && !favoritesOnly,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@Composable
private fun RepoListContent(
    assets: List<RootfsAsset>,
    isFiltered: Boolean,
    downloadStates: Map<String, AssetDownloadState>,
    favoriteUrls: Set<String>,
    onDownload: (RootfsAsset) -> Unit,
    onCancel: (RootfsAsset) -> Unit,
    onInstall: (Uri) -> Unit,
    onRetry: (RootfsAsset) -> Unit,
    onToggleFavorite: (RootfsAsset) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(assets, key = { it.downloadUrl }) { asset ->
            RootfsAssetCard(
                asset      = asset,
                state      = downloadStates[asset.downloadUrl] ?: AssetDownloadState.Idle,
                isFavorite = asset.downloadUrl in favoriteUrls,
                onDownload = { onDownload(asset) },
                onCancel   = { onCancel(asset) },
                onInstall  = onInstall,
                onRetry    = { onRetry(asset) },
                onToggleFavorite = { onToggleFavorite(asset) }
            )
        }
        // Footer: banner only when not filtering
        if (!isFiltered) {
            item {
                Spacer(Modifier.height(8.dp))
                RepoSourceBanner()
                Spacer(Modifier.height(12.dp))
            }
        } else {
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun RootfsAssetCard(
    asset: RootfsAsset,
    state: AssetDownloadState,
    isFavorite: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: (Uri) -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val cardShape = CardShape
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(asset.downloadUrl) { visible = true }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = AnimationUtils.cardFadeSpec()) +
            expandVertically(animationSpec = AnimationUtils.mediumSpec()),
        exit = fadeOut(animationSpec = AnimationUtils.fadeOutSpec())
    ) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .animateContentSize(animationSpec = AnimationUtils.mediumSpec()),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            when (state) {
                is AssetDownloadState.Done   -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                is AssetDownloadState.Failed -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                is AssetDownloadState.Verifying -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                else                         -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            }
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = IconUtils.getDistroIcon(asset.displayDistro.ifBlank { asset.name }),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (state is AssetDownloadState.Done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    var nameFontSize by remember(asset.name) { mutableStateOf(16.sp) }
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = nameFontSize),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        onTextLayout = { if (it.hasVisualOverflow) nameFontSize = (nameFontSize.value - 1).sp }
                    )
                }

                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = context.getString(R.string.repo_favorite),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }

                val (displayLabel, statusColor) = when (state) {
                    is AssetDownloadState.Done        -> context.getString(R.string.repo_status_ready) to MaterialTheme.colorScheme.primary
                    is AssetDownloadState.Downloading -> context.getString(R.string.repo_status_downloading) to MaterialTheme.colorScheme.tertiary
                    is AssetDownloadState.Verifying   -> context.getString(R.string.repo_status_verifying) to MaterialTheme.colorScheme.tertiary
                    is AssetDownloadState.Failed      -> context.getString(R.string.repo_status_failed) to MaterialTheme.colorScheme.error
                    else                              -> "" to MaterialTheme.colorScheme.primary
                }

                if (displayLabel.isNotEmpty()) {
                    StatusPill(label = displayLabel, color = statusColor)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = asset.sourceRepoName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (asset.version.isNotBlank()) {
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = asset.version,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = asset.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }

            if (asset.description.isNotEmpty()) {
                Text(
                    text = asset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (asset.sizeBytes > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatSize(asset.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    val arch = asset.architecture
                    if (arch.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = arch,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (asset.buildDate.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = formatBuildDate(asset.buildDate),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (state is AssetDownloadState.Downloading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${state.percent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            if (state is AssetDownloadState.Verifying) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }

            if (state is AssetDownloadState.Failed) {
                Text(
                    text = state.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            val btnColor: Color
            val accentColor: Color
            val btnIcon: androidx.compose.ui.graphics.vector.ImageVector
            val btnText: String
            val onClickAction: () -> Unit
            val enabled: Boolean

            when (state) {
                is AssetDownloadState.Idle -> {
                    btnColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    accentColor = MaterialTheme.colorScheme.primary
                    btnIcon = Icons.Default.CloudDownload
                    btnText = context.getString(R.string.repo_download)
                    onClickAction = onDownload
                    enabled = true
                }
                is AssetDownloadState.Downloading -> {
                    btnColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    accentColor = MaterialTheme.colorScheme.error
                    btnIcon = Icons.Default.Close
                    btnText = context.getString(R.string.repo_cancel)
                    onClickAction = onCancel
                    enabled = true
                }
                is AssetDownloadState.Verifying -> {
                    btnColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    accentColor = MaterialTheme.colorScheme.tertiary
                    btnIcon = Icons.Default.Verified
                    btnText = context.getString(R.string.repo_status_verifying)
                    onClickAction = {}
                    enabled = false
                }
                is AssetDownloadState.Done -> {
                    btnColor = MaterialTheme.colorScheme.primary
                    accentColor = MaterialTheme.colorScheme.onPrimary
                    btnIcon = Icons.Default.InstallMobile
                    btnText = context.getString(R.string.repo_install)
                    onClickAction = { onInstall(state.uri) }
                    enabled = true
                }
                is AssetDownloadState.Failed -> {
                    btnColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    accentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    btnIcon = Icons.Default.Refresh
                    btnText = context.getString(R.string.repo_retry)
                    onClickAction = onRetry
                    enabled = true
                }
            }

            Surface(
                onClick = onClickAction,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = ActionButtonShape,
                color = btnColor,
                border = if (state !is AssetDownloadState.Done) BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)) else null
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = btnIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accentColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = btnText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun RepoSourceBanner() {
    val context = LocalContext.current
    val url = context.getString(R.string.repo_banner_url)

    Surface(
        onClick = {
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = context.getString(R.string.repo_banner_text),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.0f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}

/** Formats \"20260525\" -> \"2026-05-25\" for display. Returns raw string if not 8 digits. */
private fun formatBuildDate(raw: String): String {
    if (raw.length == 8 && raw.all { it.isDigit() }) {
        return "${raw.substring(0, 4)}-${raw.substring(4, 6)}-${raw.substring(6, 8)}"
    }
    return raw
}

@Composable
private fun RepoManagerDialog(
    initialRepos: List<Pair<String, String>>,
    includeCommunity: Boolean,
    onCommunityChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (toAdd: List<Pair<String, String>>, toRemove: List<String>) -> Unit
) {
    val context = LocalContext.current

    var repos by remember { mutableStateOf(initialRepos) }
    val originalUrls = remember { initialRepos.map { it.second }.toSet() }
    var communityEnabled by remember { mutableStateOf(includeCommunity) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    var newName   by remember { mutableStateOf("") }
    var newUrl    by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf("") }
    var urlError  by remember { mutableStateOf("") }

    val fieldShape  = RoundedCornerShape(16.dp)
    val fieldColors = DsTextFieldDefaults.surfaceColors()
    val subscribedUrls = remember(repos) { repos.map { it.second }.toSet() }

    val filteredPresets = remember(categoryFilter) {
        CuratedRootfsRepos.presets.filter { categoryFilter == null || it.category == categoryFilter }
    }

    fun tryAdd(name: String = newName, urlRaw: String = newUrl) {
        val resolved = CuratedRootfsRepos.resolveGithubInput(urlRaw.trim())
        val n = name.trim().ifBlank { resolved?.let { CuratedRootfsRepos.suggestName(urlRaw) }.orEmpty() }
        nameError = if (n.isEmpty()) context.getString(R.string.repo_custom_name_empty) else ""
        urlError = when {
            urlRaw.trim().isEmpty() -> context.getString(R.string.repo_custom_url_empty)
            resolved == null -> context.getString(R.string.repo_custom_url_invalid)
            !resolved.startsWith("https://") -> context.getString(R.string.repo_custom_url_invalid)
            repos.any { it.second == resolved } -> context.getString(R.string.repo_custom_already_added)
            else -> ""
        }
        if (nameError.isEmpty() && urlError.isEmpty() && resolved != null) {
            repos = repos + (n to resolved)
            newName = ""; newUrl = ""
        }
    }

    fun togglePreset(preset: CuratedRootfsRepo) {
        repos = if (repos.any { it.second == preset.url }) {
            repos.filter { it.second != preset.url }
        } else {
            repos + (preset.name to preset.url)
        }
    }

    fun applyQuickFill(ownerRepo: String) {
        newUrl = ownerRepo
        if (newName.isBlank()) newName = CuratedRootfsRepos.suggestName(ownerRepo)
        urlError = ""
        nameError = ""
    }

    DsDialog(
        onDismiss = onDismiss,
        modifier = Modifier.imePadding(),
        scrollableContent = false,
        footer = {
            DialogFooterRow(
                dismissLabel = context.getString(R.string.cancel),
                confirmLabel = context.getString(R.string.ok),
                onDismiss = onDismiss,
                onConfirm = {
                    onCommunityChange(communityEnabled)
                    val currentUrls = repos.map { it.second }.toSet()
                    val toRemove = originalUrls.filter { it !in currentUrls }
                    val toAdd = repos.filter { it.second !in originalUrls }
                    onSave(toAdd, toRemove)
                },
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = context.getString(R.string.repo_manage_custom),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = context.getString(R.string.repo_manager_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            // Built-in LXC community toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.repo_community_feed),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = context.getString(R.string.repo_community_feed_summary),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = communityEnabled,
                        onCheckedChange = { communityEnabled = it }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = context.getString(R.string.repo_presets_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = context.getString(R.string.repo_presets_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null },
                        label = { Text(context.getString(R.string.repo_filter_all)) },
                        shape = PillShape
                    )
                }
                item {
                    FilterChip(
                        selected = categoryFilter == "official",
                        onClick = { categoryFilter = if (categoryFilter == "official") null else "official" },
                        label = { Text(context.getString(R.string.repo_cat_official)) },
                        shape = PillShape
                    )
                }
                item {
                    FilterChip(
                        selected = categoryFilter == "lxc",
                        onClick = { categoryFilter = if (categoryFilter == "lxc") null else "lxc" },
                        label = { Text(context.getString(R.string.repo_cat_lxc)) },
                        shape = PillShape
                    )
                }
                item {
                    FilterChip(
                        selected = categoryFilter == "community",
                        onClick = { categoryFilter = if (categoryFilter == "community") null else "community" },
                        label = { Text(context.getString(R.string.repo_cat_community)) },
                        shape = PillShape
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPresets, key = { it.id }) { preset ->
                    val selected = preset.url in subscribedUrls
                    Surface(
                        onClick = { togglePreset(preset) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (preset.category) {
                                    "official" -> Icons.Default.Verified
                                    "lxc" -> Icons.Default.Inventory2
                                    else -> Icons.Default.Code
                                },
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${preset.author} · ${preset.description}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            if (repos.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = context.getString(R.string.repo_subscribed_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(repos, key = { _, item -> item.second }) { _, (repoName, repoUrl) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = repoName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = repoUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { repos = repos.filter { it.second != repoUrl } },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = context.getString(R.string.repo_custom_remove),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(Modifier.height(12.dp))

            Text(
                text = context.getString(R.string.repo_add_github),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = context.getString(R.string.repo_github_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                items(CuratedRootfsRepos.githubQuickFills, key = { it.first }) { (ownerRepo, label) ->
                    AssistChip(
                        onClick = { applyQuickFill(ownerRepo) },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = PillShape
                    )
                }
            }

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it; nameError = "" },
                label = { Text(context.getString(R.string.repo_custom_name_hint)) },
                isError = nameError.isNotEmpty(),
                supportingText = if (nameError.isNotEmpty()) { { Text(nameError) } } else null,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = newUrl,
                onValueChange = {
                    newUrl = it
                    urlError = ""
                    if (newName.isBlank()) newName = CuratedRootfsRepos.suggestName(it)
                },
                label = { Text(context.getString(R.string.repo_custom_url_hint)) },
                isError = urlError.isNotEmpty(),
                supportingText = if (urlError.isNotEmpty()) { { Text(urlError) } } else null,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                onClick = { tryAdd() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = context.getString(R.string.repo_custom_add),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = context.getString(R.string.repo_custom_add),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}


@Composable
private fun RepoSearchBar(query: String, onQueryChange: (String) -> Unit) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        label = "searchBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            placeholder = {
                Text(
                    text = context.getString(R.string.search) + "...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isFocused) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = FocusUtils.searchKeyboardOptions,
            keyboardActions = FocusUtils.clearFocusKeyboardActions()
        )
    }
}


