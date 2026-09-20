package com.droidspaces.app.ui.screen
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.Color

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.droidspaces.app.ui.component.PullToRefreshWrapper
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.droidspaces.app.ui.component.DroidspacesStatus
import com.droidspaces.app.ui.component.DroidspacesStatusCard
import com.droidspaces.app.ui.component.SystemInfoCard
import com.droidspaces.app.util.DroidspacesBackendStatus
import com.droidspaces.app.util.PreferencesManager
import com.droidspaces.app.util.SystemInfoManager
import com.droidspaces.app.ui.viewmodel.AppStateViewModel
import com.droidspaces.app.ui.viewmodel.ContainerViewModel
import com.droidspaces.app.ui.component.HelpCard
import com.droidspaces.app.util.AnimationUtils
import com.droidspaces.app.util.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.droidspaces.app.util.CuratedRootfsRepos
import com.droidspaces.app.ui.theme.ThemePalette
import com.droidspaces.app.ui.theme.rememberThemeState
import com.droidspaces.app.ui.component.AccentColorPicker
import com.droidspaces.app.ui.component.CustomThemeEditorDialog
import com.droidspaces.app.ui.component.applyCustomTheme
import com.droidspaces.app.ui.component.ColorPaletteSwatch
import com.droidspaces.app.R

private const val EASTER_EGG_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

enum class TabItem(val titleResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home(R.string.home_title, Icons.Default.Home),
    Containers(R.string.containers, Icons.Default.Storage),
    ControlPanel(R.string.panel, Icons.Default.Dashboard)
}

/**
 * Backend is only checked on:
 * 1. Cold app start
 * 2. Pull-to-refresh
 * 3. Post-installation (when returning from installation flow)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainTabScreen(
    appStateViewModel: AppStateViewModel,
    containerViewModel: ContainerViewModel,
    skipInitialRefresh: Boolean = false,
    requestedTab: TabItem? = null,
    onRequestedTabConsumed: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInstallation: () -> Unit = {},
    onNavigateToContainerInstallation: (android.net.Uri) -> Unit = {},
    onNavigateToEditContainer: (String) -> Unit = {},
    onNavigateToContainerDetails: (String) -> Unit = {},
    onNavigateToTerminal: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // containerViewModel is now passed as parameter to ensure sharing


    val tabs = remember { TabItem.values() }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val selectedTab = tabs[pagerState.currentPage]

    // Jump to the tab requested by a launcher shortcut, then clear the request.
    LaunchedEffect(requestedTab) {
        val target = requestedTab ?: return@LaunchedEffect
        val index = tabs.indexOf(target)
        if (index >= 0) {
            pagerState.scrollToPage(index)
        }
        onRequestedTabConsumed()
    }

    // Track which container has its action drawer expanded (hoisted for global collapse)
    var expandedContainerName by rememberSaveable { mutableStateOf<String?>(null) }

    // Handle back press - return to Home tab if not already there
    BackHandler(enabled = selectedTab != TabItem.Home) {
        scope.launch {
            pagerState.scrollToPage(tabs.indexOf(TabItem.Home))
        }
    }

    // Track if we've already triggered initial load in this session
    var hasTriggeredInitialLoad by rememberSaveable { mutableStateOf(false) }

    // Initial setup - only runs ONCE per app session
    // DO NOT use hasTriggeredInitialLoad as a key, because changing it cancels the effect!
    LaunchedEffect(skipInitialRefresh) {
        if (!hasTriggeredInitialLoad) {
            hasTriggeredInitialLoad = true

            if (!skipInitialRefresh) {
                // Post-installation: reset and force refresh
                appStateViewModel.resetForPostInstallation()
            }

            // Always force check on initial boot
            appStateViewModel.checkBackendStatus(force = true)

            // Proactive recovery on first launch: Always run scan if backend available
            if (appStateViewModel.isBackendAvailable) {
                containerViewModel.runScan()
            } else {
                // If not available immediately, ensure we fetch when it does become available
                containerViewModel.fetchContainerList()
            }
        }
    }

    // Fetch containers when backend BECOMES available or evaluates to true
    LaunchedEffect(appStateViewModel.isBackendAvailable) {
        if (appStateViewModel.isBackendAvailable) {
            // Because runScan might be running, fetchContainerList is safe because it only
            // cancels previous fetch jobs, and runs concurrently (which is fine, UI populates fast).
            // We only trigger this if it's currently empty, to avoid double-fetching if runScan succeeded,
            // or just always run it since it's cheap and ensures sync.
            if (containerViewModel.containerList.isEmpty()) {
                containerViewModel.fetchContainerList()
            }
        }
    }

    // Map backend status to UI status - remember previous status to prevent glitches
    val currentBackendStatus = appStateViewModel.backendStatus
    val prefsManager = remember { PreferencesManager.getInstance(context) }

    // Initialize stable status from cached backend status to prevent initial boot glitch
    val stableDroidspacesStatus = remember {
        mutableStateOf<DroidspacesStatus?>(
            prefsManager.cachedBackendStatus?.let { cached ->
                when (cached) {
                    "UpdateAvailable" -> DroidspacesStatus.UpdateAvailable
                    "NotInstalled" -> DroidspacesStatus.NotInstalled
                    "Corrupted" -> DroidspacesStatus.Corrupted
                    "ModuleMissing" -> DroidspacesStatus.ModuleMissing
                    "" -> DroidspacesStatus.Working
                    else -> null
                }
            }
        )
    }

    // Track previous root status to detect when root becomes unavailable
    var previousRootAvailable by remember { mutableStateOf(appStateViewModel.isRootAvailable) }

    // Update stable status when backend status changes (but skip Checking to prevent flicker)
    LaunchedEffect(currentBackendStatus, appStateViewModel.isRootAvailable) {
        // Skip updates during Checking state to prevent flicker during refresh
        if (currentBackendStatus is DroidspacesBackendStatus.Checking) {
            return@LaunchedEffect
        }

        val newStatus = when (currentBackendStatus) {
            is DroidspacesBackendStatus.Available -> DroidspacesStatus.Working
            is DroidspacesBackendStatus.UpdateAvailable -> DroidspacesStatus.UpdateAvailable
            is DroidspacesBackendStatus.NotInstalled -> DroidspacesStatus.NotInstalled
            is DroidspacesBackendStatus.Corrupted -> DroidspacesStatus.Corrupted
            is DroidspacesBackendStatus.ModuleMissing -> DroidspacesStatus.ModuleMissing
            is DroidspacesBackendStatus.Checking -> return@LaunchedEffect // Already handled above
        }

        // Always update stable status when we have a non-Checking status
        // This allows updates from error to working (e.g., after installation)
        stableDroidspacesStatus.value = newStatus

        previousRootAvailable = appStateViewModel.isRootAvailable
    }

    // Use stable status if available, otherwise compute from current status
    val droidspacesStatus: DroidspacesStatus = stableDroidspacesStatus.value ?: when (currentBackendStatus) {
        is DroidspacesBackendStatus.Checking -> DroidspacesStatus.Working
        is DroidspacesBackendStatus.Available -> DroidspacesStatus.Working
        is DroidspacesBackendStatus.UpdateAvailable -> DroidspacesStatus.UpdateAvailable
        is DroidspacesBackendStatus.NotInstalled -> DroidspacesStatus.NotInstalled
        is DroidspacesBackendStatus.Corrupted -> DroidspacesStatus.Corrupted
        is DroidspacesBackendStatus.ModuleMissing -> DroidspacesStatus.ModuleMissing
    }

    // UI state from ViewModels
    val isBackendAvailable = appStateViewModel.isBackendAvailable
    // Only show checking on initial load when we don't have cached status
    val isChecking = !appStateViewModel.hasCompletedInitialCheck && stableDroidspacesStatus.value == null
    val containerCount = containerViewModel.containerCount
    val runningCount = containerViewModel.runningCount

    /**
     * Combined refresh function for pull-to-refresh.
     * Refreshes both backend status and container list with specialized logic per tab.
     */
    suspend fun performRefresh(tab: TabItem) {
        // Check root status first (in case user denied root access)
        appStateViewModel.checkRootStatus()
        // Then refresh backend status
        appStateViewModel.forceRefresh()
        // Force refresh droidspaces version to get latest after backend updates
        if (appStateViewModel.isBackendAvailable) {
            SystemInfoManager.refreshDroidspacesVersion(context)
            SystemInfoManager.refreshBackendMode(context)

            when (tab) {
                TabItem.Home, TabItem.Containers -> {
                    // Home/Containers: Always run a full scan on refresh for maximum visibility
                    containerViewModel.runScan()
                }
                TabItem.ControlPanel -> {
                    // Control Panel: snappy refresh, but background recovery
                    val rawList = withContext(Dispatchers.IO) {
                        com.droidspaces.app.util.ContainerManager.listContainers()
                    }
                    val anyRunning = rawList.any { it.isRunning }

                    if (!anyRunning) {
                        // Metadata missing for running containers? attempt foreground recovery
                        containerViewModel.runScan()
                    } else {
                        // Running containers found, update UI normally (snappy)
                        withContext(Dispatchers.Main) {
                            containerViewModel.updateState(rawList)
                        }
                        // Then scan in background silently to catch orphans
                        scope.launch {
                            containerViewModel.silentScan()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { expandedContainerName = null }
                    ) {
                        Icon(
                            imageVector = Icons.Default.BlurOn,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = context.getString(R.string.app_name),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = context.getString(selectedTab.titleResId),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = context.getString(R.string.settings))
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            // No bottom bar content here to avoid the solid background
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        val density = LocalDensity.current
        // Measured height of the floating bottom bar (incl. system nav inset).
        // Reserved as bottom space for centered empty states so they sit in the
        // visible region above the bar instead of behind it.
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        var openRepoSheetRequest by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    TabItem.Home -> {
                        HomeTabContent(
                            appUpdate = appStateViewModel.appUpdate,
                            droidspacesStatus = droidspacesStatus,
                            isChecking = isChecking,
                            isRootAvailable = appStateViewModel.isRootAvailable,
                            onNavigateToInstallation = onNavigateToInstallation,
                            onNavigateToContainers = {
                                scope.launch {
                                    pagerState.scrollToPage(tabs.indexOf(TabItem.Containers))
                                }
                            },
                            onNavigateToControlPanel = {
                                scope.launch {
                                    pagerState.scrollToPage(tabs.indexOf(TabItem.ControlPanel))
                                }
                            },
                            onNavigateToRootfsRepo = {
                                openRepoSheetRequest = true
                                scope.launch {
                                    pagerState.scrollToPage(tabs.indexOf(TabItem.Containers))
                                }
                            },
                            containerCount = containerCount,
                            runningCount = runningCount,
                            onRefresh = { performRefresh(TabItem.Home) }
                        )
                    }

                    TabItem.Containers -> {
                        ContainersTabContent(
                            isBackendAvailable = isBackendAvailable,
                            isRootAvailable = appStateViewModel.isRootAvailable,
                            onNavigateToInstallation = onNavigateToContainerInstallation,
                            onNavigateToEditContainer = onNavigateToEditContainer,
                            onNavigateToContainerDetails = onNavigateToContainerDetails,
                            containerViewModel = containerViewModel,
                            onRefresh = { performRefresh(TabItem.Containers) },
                            expandedContainerName = expandedContainerName,
                            onExpandedContainerNameChange = { expandedContainerName = it },
                            emptyStateBottomInset = bottomBarHeight,
                            openRepoSheet = openRepoSheetRequest,
                            onOpenRepoSheetConsumed = { openRepoSheetRequest = false }
                        )
                    }

                    TabItem.ControlPanel -> {
                        ControlPanelTabContent(
                            isBackendAvailable = isBackendAvailable,
                            isRootAvailable = appStateViewModel.isRootAvailable,
                            containerViewModel = containerViewModel,
                            onRefresh = { performRefresh(TabItem.ControlPanel) },
                            onNavigateToContainerDetails = onNavigateToContainerDetails,
                            onNavigateToTerminal = onNavigateToTerminal,
                            emptyStateBottomInset = bottomBarHeight
                        )
                    }
                }
            }

            // Floating Bottom Bar Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } }
            ) {
                MainBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        scope.launch {
                            pagerState.scrollToPage(tabs.indexOf(tab))
                        }
                        expandedContainerName = null
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    appUpdate: AppUpdateInfo?,
    droidspacesStatus: DroidspacesStatus,
    isChecking: Boolean,
    isRootAvailable: Boolean,
    onNavigateToInstallation: () -> Unit,
    onNavigateToContainers: () -> Unit,
    onNavigateToControlPanel: () -> Unit,
    onNavigateToRootfsRepo: () -> Unit,
    containerCount: Int,
    runningCount: Int,
    onRefresh: suspend () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    val themeState = rememberThemeState()
    var refreshTrigger by remember { mutableStateOf(0) }
    var idleTaps by remember(droidspacesStatus) { mutableStateOf(0) }
    var showThemeEditor by remember { mutableStateOf(false) }

    var mastheadVisible by remember { mutableStateOf(false) }
    var gridVisible by remember { mutableStateOf(false) }
    var repoVisible by remember { mutableStateOf(false) }
    var themeVisible by remember { mutableStateOf(false) }
    var restVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(20); mastheadVisible = true
        delay(70); gridVisible = true
        delay(70); repoVisible = true
        delay(70); themeVisible = true
        delay(70); restVisible = true
    }

    val mastAlpha by animateFloatAsState(
        if (mastheadVisible) 1f else 0f,
        AnimationUtils.fadeInSpec(),
        label = "mastA"
    )
    val gridAlpha by animateFloatAsState(
        if (gridVisible) 1f else 0f,
        AnimationUtils.fadeInSpec(),
        label = "gridA"
    )
    val repoAlpha by animateFloatAsState(
        if (repoVisible) 1f else 0f,
        AnimationUtils.fadeInSpec(),
        label = "repoA"
    )
    val themeAlpha by animateFloatAsState(
        if (themeVisible) 1f else 0f,
        AnimationUtils.fadeInSpec(),
        label = "themeA"
    )
    val restAlpha by animateFloatAsState(
        if (restVisible) 1f else 0f,
        AnimationUtils.fadeInSpec(),
        label = "restA"
    )

    val animatedContainers by animateIntAsState(
        targetValue = containerCount,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "containers"
    )
    val animatedRunning by animateIntAsState(
        targetValue = runningCount,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "running"
    )

    PullToRefreshWrapper(
        onRefresh = {
            onRefresh()
            refreshTrigger++
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Masthead — brand + live counts, not a card stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(mastAlpha)
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = context.getString(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = context.getString(R.string.home_command_center),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = context.getString(
                        R.string.home_spaces_live,
                        animatedContainers,
                        animatedRunning
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            // Bento: status spans full width, then two metric tiles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(gridAlpha)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DroidspacesStatusCard(
                    status = droidspacesStatus,
                    version = null,
                    isChecking = isChecking,
                    isRootAvailable = isRootAvailable,
                    refreshTrigger = refreshTrigger,
                    appUpdate = appUpdate,
                    onClick = {
                        if (!isRootAvailable) return@DroidspacesStatusCard
                        if (droidspacesStatus == DroidspacesStatus.NotInstalled ||
                            droidspacesStatus == DroidspacesStatus.Corrupted ||
                            droidspacesStatus == DroidspacesStatus.UpdateAvailable ||
                            droidspacesStatus == DroidspacesStatus.ModuleMissing
                        ) {
                            onNavigateToInstallation()
                            return@DroidspacesStatusCard
                        }
                        if (droidspacesStatus != DroidspacesStatus.Working) return@DroidspacesStatusCard
                        idleTaps++
                        when (idleTaps) {
                            5 -> Toast.makeText(context, R.string.easter_egg_warning, Toast.LENGTH_SHORT).show()
                            10 -> {
                                idleTaps = 0
                                Toast.makeText(context, R.string.easter_egg_reward, Toast.LENGTH_SHORT).show()
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(EASTER_EGG_URL)))
                                }
                            }
                        }
                    }
                )

                if (isRootAvailable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeMetricTile(
                            modifier = Modifier.weight(1f),
                            value = animatedContainers,
                            label = context.getString(R.string.containers),
                            hint = context.getString(R.string.home_metric_containers_hint),
                            icon = Icons.Default.Layers,
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToContainers
                        )
                        HomeMetricTile(
                            modifier = Modifier.weight(1f),
                            value = animatedRunning,
                            label = context.getString(R.string.running),
                            hint = context.getString(R.string.home_metric_running_hint),
                            icon = Icons.Default.PlayCircle,
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = onNavigateToControlPanel
                        )
                    }
                }
            }

            // Rootfs Repository — featured catalogs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(repoAlpha)
                    .padding(top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.home_rootfs_section),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = context.getString(R.string.home_rootfs_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(CuratedRootfsRepos.presets, key = { it.id }) { repo ->
                        HomeRootfsRepoCard(
                            name = repo.name,
                            description = repo.description,
                            category = repo.category,
                            onClick = onNavigateToRootfsRepo
                        )
                    }
                    item(key = "browse-all") {
                        Surface(
                            onClick = onNavigateToRootfsRepo,
                            modifier = Modifier
                                .width(148.dp)
                                .height(168.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = context.getString(R.string.home_rootfs_browse_all),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Appearance dock — theme mode + palettes + create
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(themeAlpha)
                    .padding(horizontal = 16.dp)
                    .padding(top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = context.getString(R.string.home_appearance_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = context.getString(R.string.home_appearance_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeThemeModeChip(
                        label = context.getString(R.string.home_theme_system),
                        selected = themeState.followSystemTheme,
                        onClick = { prefsManager.followSystemTheme = true },
                        modifier = Modifier.weight(1f)
                    )
                    HomeThemeModeChip(
                        label = context.getString(R.string.home_theme_light),
                        selected = !themeState.followSystemTheme && !prefsManager.darkTheme,
                        onClick = {
                            prefsManager.followSystemTheme = false
                            prefsManager.darkTheme = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HomeThemeModeChip(
                        label = context.getString(R.string.home_theme_dark),
                        selected = !themeState.followSystemTheme && prefsManager.darkTheme,
                        onClick = {
                            prefsManager.followSystemTheme = false
                            prefsManager.darkTheme = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    AccentColorPicker(
                        selectedPalette = themeState.themePalette,
                        isDarkTheme = themeState.darkTheme,
                        customThemeColors = themeState.customThemeColors,
                        onPaletteSelected = { palette ->
                            prefsManager.useDynamicColor = false
                            prefsManager.themePalette = palette.name
                        },
                        onCustomColorsApplied = { }
                    )
                }

                Surface(
                    onClick = { showThemeEditor = true },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = context.getString(R.string.theme_create_cta),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = context.getString(R.string.theme_create_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 2
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(restAlpha)
                    .padding(horizontal = 16.dp)
                    .padding(top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRootAvailable) {
                    HomeQuickAction(
                        icon = Icons.Default.Dashboard,
                        label = context.getString(R.string.panel),
                        description = context.getString(R.string.home_quick_panel_desc),
                        onClick = onNavigateToControlPanel
                    )
                }
                SystemInfoCard(refreshTrigger = refreshTrigger)
                HelpCard()
            }
        }
    }

    if (showThemeEditor) {
        CustomThemeEditorDialog(
            initial = themeState.customThemeColors,
            isDarkTheme = themeState.darkTheme,
            onDismiss = { showThemeEditor = false },
            onApply = { colors ->
                applyCustomTheme(prefsManager, colors)
                showThemeEditor = false
            }
        )
    }
}

@Composable
private fun HomeRootfsRepoCard(
    name: String,
    description: String,
    category: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(168.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
            ) {
                Text(
                    text = category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    letterSpacing = 0.8.sp
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun HomeThemeModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
    val fill = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(14.dp),
        color = fill,
        border = border
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HomeMetricTile(
    value: Int,
    label: String,
    hint: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(136.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.15f), accent, accent.copy(alpha = 0.2f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContainersTabContent(

    isBackendAvailable: Boolean,
    isRootAvailable: Boolean,
    onNavigateToInstallation: (android.net.Uri) -> Unit,
    onNavigateToEditContainer: (String) -> Unit,
    onNavigateToContainerDetails: (String) -> Unit,
    containerViewModel: ContainerViewModel,
    onRefresh: suspend () -> Unit,
    expandedContainerName: String?,
    onExpandedContainerNameChange: (String?) -> Unit,
    emptyStateBottomInset: Dp = 0.dp,
    openRepoSheet: Boolean = false,
    onOpenRepoSheetConsumed: () -> Unit = {}
) {
    PullToRefreshWrapper(onRefresh = { onRefresh() }) {
        ContainersScreen(
            isBackendAvailable = isBackendAvailable,
            isRootAvailable = isRootAvailable,
            onNavigateToInstallation = onNavigateToInstallation,
            onNavigateToEditContainer = onNavigateToEditContainer,
            onNavigateToContainerDetails = onNavigateToContainerDetails,
            containerViewModel = containerViewModel,
            expandedContainerName = expandedContainerName,
            onExpandedContainerNameChange = onExpandedContainerNameChange,
            emptyStateBottomInset = emptyStateBottomInset,
            openRepoSheet = openRepoSheet,
            onOpenRepoSheetConsumed = onOpenRepoSheetConsumed
        )
    }
}

@Composable
private fun HomeQuickAction(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ControlPanelTabContent(
    isBackendAvailable: Boolean,
    isRootAvailable: Boolean,
    containerViewModel: ContainerViewModel,
    onRefresh: suspend () -> Unit,
    onNavigateToContainerDetails: (String) -> Unit,
    onNavigateToTerminal: (String) -> Unit,
    emptyStateBottomInset: Dp = 0.dp
) {
    var refreshTrigger by remember { mutableStateOf(0) }

    PullToRefreshWrapper(onRefresh = {
        onRefresh()
        refreshTrigger++
    }) {
        ControlPanelScreen(
            isBackendAvailable = isBackendAvailable,
            isRootAvailable = isRootAvailable,
            containerViewModel = containerViewModel,
            onNavigateToContainerDetails = onNavigateToContainerDetails,
            onNavigateToTerminal = onNavigateToTerminal,
            emptyStateBottomInset = emptyStateBottomInset
        )
    }
}

@Composable
private fun MainBottomBar(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit
) {
    val context = LocalContext.current
    val tabs = TabItem.entries
    val selectedIndex = tabs.indexOf(selectedTab)

            // Floating capsule nav — inset from screen edges for a modern dock look.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = com.droidspaces.app.ui.theme.NavBarShape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
            ),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tabWidth = maxWidth / tabs.size
                    val offset by androidx.compose.animation.core.animateDpAsState(
                        targetValue = tabWidth * selectedIndex,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        label = "IndicatorOffset"
                    )

                    Surface(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .offset(x = offset),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        )
                    ) {}
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val contentColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            label = "IconColor"
                        )

                        Surface(
                            onClick = { onTabSelected(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (isSelected) 24.dp else 22.dp),
                                    tint = contentColor
                                )
                                Text(
                                    text = context.getString(tab.titleResId),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
