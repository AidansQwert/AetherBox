package com.droidspaces.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidspaces.app.R
import com.droidspaces.app.ui.component.AccentColorPicker
import com.droidspaces.app.ui.component.CustomThemeEditorDialog
import com.droidspaces.app.ui.component.DroidspacesStatus
import com.droidspaces.app.ui.component.DroidspacesStatusCard
import com.droidspaces.app.ui.component.HelpCard
import com.droidspaces.app.ui.component.PullToRefreshWrapper
import com.droidspaces.app.ui.component.SystemInfoCard
import com.droidspaces.app.ui.component.applyCustomTheme
import com.droidspaces.app.ui.theme.AdaptiveMetrics
import com.droidspaces.app.ui.theme.HomeLayout
import com.droidspaces.app.ui.theme.JetBrainsMono
import com.droidspaces.app.ui.theme.SpaceGrotesk
import com.droidspaces.app.ui.theme.ThemeState
import com.droidspaces.app.ui.theme.WidthClass
import com.droidspaces.app.ui.theme.rememberAdaptiveMetrics
import com.droidspaces.app.ui.theme.rememberHomeLayout
import com.droidspaces.app.ui.theme.rememberThemeState
import com.droidspaces.app.ui.viewmodel.AppStateViewModel
import com.droidspaces.app.ui.viewmodel.ContainerViewModel
import com.droidspaces.app.util.AppUpdateInfo
import com.droidspaces.app.util.CuratedRootfsRepos
import com.droidspaces.app.util.DroidspacesBackendStatus
import com.droidspaces.app.util.HomeOrbitActionId
import com.droidspaces.app.util.HomeOrbitCatalog
import com.droidspaces.app.util.PreferencesManager
import androidx.compose.foundation.combinedClickable
import com.droidspaces.app.util.SystemInfoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val adaptive = rememberAdaptiveMetrics()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { expandedContainerName = null }
                    ) {
                        if (selectedTab != TabItem.Home) {
                            Icon(
                                imageVector = Icons.Default.BlurOn,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            if (selectedTab != TabItem.Home) {
                                Text(
                                    text = context.getString(R.string.app_name),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp
                                )
                            }
                            Text(
                                text = context.getString(selectedTab.titleResId),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold
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
            // Phone: floating dock overlay inside content Box.
            // Tablet/rail: side NavigationRail — no bottom bar.
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        val density = LocalDensity.current
        // Measured height of the floating bottom bar (incl. system nav inset).
        // Reserved as bottom space for centered empty states so they sit in the
        // visible region above the bar instead of behind it.
        var bottomBarHeight by remember { mutableStateOf(0.dp) }
        var openRepoSheetRequest by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (adaptive.useRail) {
                MainSideRail(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        scope.launch {
                            pagerState.scrollToPage(tabs.indexOf(tab))
                        }
                        expandedContainerName = null
                    }
                )
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !adaptive.useRail
                ) { page ->
                    when (tabs[page]) {
                        TabItem.Home -> {
                            HomeTabContent(
                                appUpdate = appStateViewModel.appUpdate,
                                onDismissAppUpdate = { appStateViewModel.dismissAppUpdate() },
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
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToContainerDetails = onNavigateToContainerDetails,
                                knownContainerNames = containerViewModel.containerList.map { it.name },
                                containerRunningMap = containerViewModel.containerList.associate { it.name to it.isRunning },
                                containerCount = containerCount,
                                runningCount = runningCount,
                                onRefresh = { performRefresh(TabItem.Home) },
                                adaptive = adaptive
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
                                emptyStateBottomInset = if (adaptive.useRail) 0.dp else bottomBarHeight,
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
                                emptyStateBottomInset = if (adaptive.useRail) 0.dp else bottomBarHeight
                            )
                        }
                    }
                }

                if (!adaptive.useRail) {
                    // Floating Bottom Bar Overlay (phones)
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
    }
}

@Composable
private fun HomeTabContent(
    appUpdate: AppUpdateInfo?,
    onDismissAppUpdate: () -> Unit,
    droidspacesStatus: DroidspacesStatus,
    isChecking: Boolean,
    isRootAvailable: Boolean,
    onNavigateToInstallation: () -> Unit,
    onNavigateToContainers: () -> Unit,
    onNavigateToControlPanel: () -> Unit,
    onNavigateToRootfsRepo: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToContainerDetails: (String) -> Unit,
    knownContainerNames: List<String>,
    containerRunningMap: Map<String, Boolean>,
    containerCount: Int,
    runningCount: Int,
    onRefresh: suspend () -> Unit,
    adaptive: AdaptiveMetrics
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager.getInstance(context) }
    val themeState = rememberThemeState()
    val homeLayout = rememberHomeLayout()
    var refreshTrigger by remember { mutableStateOf(0) }
    var idleTaps by remember(droidspacesStatus) { mutableStateOf(0) }
    var showThemeEditor by remember { mutableStateOf(false) }
    var showLook by remember { mutableStateOf(homeLayout.showAppearanceExpanded()) }
    var orbitSlotsRaw by remember { mutableStateOf(prefsManager.homeOrbitSlots) }
    var pinnedRootfs by remember { mutableStateOf(prefsManager.pinnedRootfsPresetIds) }
    val lastContainer = prefsManager.lastContainerName
    val resumeTarget = lastContainer.takeIf { it.isNotBlank() && it in knownContainerNames }

    LaunchedEffect(homeLayout) {
        showLook = homeLayout.showAppearanceExpanded()
    }

    val sectionGap = homeLayout.sectionGap()
    val horizontalPad = homeLayout.horizontalPad()
    val useSplit = (homeLayout == HomeLayout.DASHBOARD && adaptive.dualPaneHome) ||
        (homeLayout == HomeLayout.GALLERY && adaptive.widthClass == WidthClass.Expanded)
    val compactMetrics = homeLayout.isCompactMetrics()
    val showOrbit = homeLayout.showOrbit() && isRootAvailable

    val animatedContainers by animateIntAsState(
        targetValue = containerCount,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "containers"
    )
    val animatedRunning by animateIntAsState(
        targetValue = runningCount,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "running"
    )

    val onStatusClick: () -> Unit = {
        if (isRootAvailable) {
            if (droidspacesStatus == DroidspacesStatus.NotInstalled ||
                droidspacesStatus == DroidspacesStatus.Corrupted ||
                droidspacesStatus == DroidspacesStatus.UpdateAvailable ||
                droidspacesStatus == DroidspacesStatus.ModuleMissing
            ) {
                onNavigateToInstallation()
            } else if (droidspacesStatus == DroidspacesStatus.Working) {
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
        }
    }

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
                .padding(bottom = if (adaptive.useRail) 28.dp else 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = adaptive.contentMaxWidth).fillMaxWidth()) {
                when (homeLayout) {
                    HomeLayout.COMMAND -> {
                        HomeCommandHero(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                    HomeLayout.COMPACT, HomeLayout.FOCUS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad)
                                .padding(top = 6.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = context.getString(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.4).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = context.getString(
                                    R.string.home_spaces_live,
                                    animatedContainers,
                                    animatedRunning
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMono,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HomeLayout.SIMPLE, HomeLayout.GALLERY, HomeLayout.DASHBOARD -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad)
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = context.getString(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.6).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = context.getString(R.string.home_command_center),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPad)
                        .padding(top = sectionGap),
                    verticalArrangement = Arrangement.spacedBy(sectionGap)
                ) {
                    if (showOrbit) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = context.getString(R.string.home_orbit_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(R.string.home_orbit_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            val orbitSlots = HomeOrbitActionId.parseSlots(orbitSlotsRaw)
                            val accents = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                orbitSlots.forEachIndexed { index, actionId ->
                                    val def = HomeOrbitCatalog.def(actionId)
                                    val enabled = when (actionId) {
                                        HomeOrbitActionId.RESUME -> resumeTarget != null
                                        else -> true
                                    }
                                    HomeOrbitAction(
                                        modifier = Modifier
                                            .weight(1f)
                                            .alpha(if (enabled) 1f else 0.45f),
                                        icon = def.icon,
                                        label = context.getString(def.labelRes),
                                        accent = accents[index % accents.size],
                                        onClick = {
                                            if (!enabled) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.home_resume_empty),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@HomeOrbitAction
                                            }
                                            when (actionId) {
                                                HomeOrbitActionId.SPACES -> onNavigateToContainers()
                                                HomeOrbitActionId.LIVE -> onNavigateToControlPanel()
                                                HomeOrbitActionId.IMAGES -> onNavigateToRootfsRepo()
                                                HomeOrbitActionId.RESUME ->
                                                    resumeTarget?.let(onNavigateToContainerDetails)
                                                HomeOrbitActionId.SETTINGS -> onNavigateToSettings()
                                            }
                                        }
                                    )
                                }
                            }
                            if (resumeTarget != null) {
                                Surface(
                                    onClick = { onNavigateToContainerDetails(resumeTarget) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = context.getString(R.string.home_resume_chip, resumeTarget),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (homeLayout == HomeLayout.COMMAND) {
                            HomeLivePulse(
                                runningCount = animatedRunning,
                                containerCount = animatedContainers
                            )
                        }
                    }

                    if (useSplit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(sectionGap)
                            ) {
                                HomeStatusAndMetricsBlock(
                                    droidspacesStatus = droidspacesStatus,
                                    isChecking = isChecking,
                                    isRootAvailable = isRootAvailable,
                                    refreshTrigger = refreshTrigger,
                                    appUpdate = appUpdate,
                                    onDismissAppUpdate = onDismissAppUpdate,
                                    onStatusClick = onStatusClick,
                                    compactMetrics = compactMetrics,
                                    animatedContainers = animatedContainers,
                                    animatedRunning = animatedRunning,
                                    onNavigateToContainers = onNavigateToContainers,
                                    onNavigateToControlPanel = onNavigateToControlPanel
                                )
                                if (isRootAvailable && homeLayout != HomeLayout.FOCUS) {
                                    HomePinnedSpacesBlock(
                                        pinnedNames = prefsManager.getPinnedContainers()
                                            .filter { it in knownContainerNames },
                                        runningMap = containerRunningMap,
                                        onOpen = onNavigateToContainerDetails,
                                        onBrowse = onNavigateToContainers
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(sectionGap)
                            ) {
                                HomeRootfsBlock(
                                    homeLayout = homeLayout,
                                    compactMetrics = compactMetrics,
                                    adaptive = adaptive,
                                    pinnedIds = pinnedRootfs,
                                    onTogglePin = { id ->
                                        pinnedRootfs = prefsManager.togglePinnedRootfsPreset(id)
                                    },
                                    onNavigateToRootfsRepo = onNavigateToRootfsRepo
                                )
                                HomeAppearanceBlock(
                                    homeLayout = homeLayout,
                                    adaptive = adaptive,
                                    showLook = showLook,
                                    onShowLook = { showLook = true },
                                    prefsManager = prefsManager,
                                    themeState = themeState,
                                    orbitSlotsRaw = orbitSlotsRaw,
                                    onOrbitSlotsChanged = { orbitSlotsRaw = it },
                                    onOpenThemeEditor = { showThemeEditor = true }
                                )
                            }
                        }
                    } else {
                        HomeStatusAndMetricsBlock(
                            droidspacesStatus = droidspacesStatus,
                            isChecking = isChecking,
                            isRootAvailable = isRootAvailable,
                            refreshTrigger = refreshTrigger,
                            appUpdate = appUpdate,
                            onDismissAppUpdate = onDismissAppUpdate,
                            onStatusClick = onStatusClick,
                            compactMetrics = compactMetrics,
                            animatedContainers = animatedContainers,
                            animatedRunning = animatedRunning,
                            onNavigateToContainers = onNavigateToContainers,
                            onNavigateToControlPanel = onNavigateToControlPanel
                        )
                        if (isRootAvailable && homeLayout != HomeLayout.FOCUS) {
                            HomePinnedSpacesBlock(
                                pinnedNames = prefsManager.getPinnedContainers()
                                    .filter { it in knownContainerNames },
                                runningMap = containerRunningMap,
                                onOpen = onNavigateToContainerDetails,
                                onBrowse = onNavigateToContainers
                            )
                        }
                        HomeRootfsBlock(
                            homeLayout = homeLayout,
                            compactMetrics = compactMetrics,
                            adaptive = adaptive,
                            pinnedIds = pinnedRootfs,
                            onTogglePin = { id ->
                                pinnedRootfs = prefsManager.togglePinnedRootfsPreset(id)
                            },
                            onNavigateToRootfsRepo = onNavigateToRootfsRepo
                        )
                        HomeAppearanceBlock(
                            homeLayout = homeLayout,
                            adaptive = adaptive,
                            showLook = showLook,
                            onShowLook = { showLook = true },
                            prefsManager = prefsManager,
                            themeState = themeState,
                            orbitSlotsRaw = orbitSlotsRaw,
                            onOrbitSlotsChanged = { orbitSlotsRaw = it },
                            onOpenThemeEditor = { showThemeEditor = true }
                        )
                    }

                    if (isRootAvailable && !showOrbit) {
                        HomeQuickAction(
                            icon = Icons.Default.Dashboard,
                            label = context.getString(R.string.panel),
                            description = context.getString(R.string.home_quick_panel_desc),
                            onClick = onNavigateToControlPanel
                        )
                    }
                    if (homeLayout != HomeLayout.FOCUS) {
                        SystemInfoCard(refreshTrigger = refreshTrigger)
                        HelpCard()
                    }
                }
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
private fun HomeStatusAndMetricsBlock(
    droidspacesStatus: DroidspacesStatus,
    isChecking: Boolean,
    isRootAvailable: Boolean,
    refreshTrigger: Int,
    appUpdate: AppUpdateInfo?,
    onDismissAppUpdate: () -> Unit,
    onStatusClick: () -> Unit,
    compactMetrics: Boolean,
    animatedContainers: Int,
    animatedRunning: Int,
    onNavigateToContainers: () -> Unit,
    onNavigateToControlPanel: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DroidspacesStatusCard(
            status = droidspacesStatus,
            version = null,
            isChecking = isChecking,
            isRootAvailable = isRootAvailable,
            refreshTrigger = refreshTrigger,
            appUpdate = appUpdate,
            onDismissAppUpdate = onDismissAppUpdate,
            onClick = onStatusClick
        )

        if (isRootAvailable) {
            if (compactMetrics) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeCompactMetricRow(
                        value = animatedContainers,
                        label = context.getString(R.string.containers),
                        hint = context.getString(R.string.home_metric_containers_hint),
                        icon = Icons.Default.Layers,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToContainers
                    )
                    HomeCompactMetricRow(
                        value = animatedRunning,
                        label = context.getString(R.string.running),
                        hint = context.getString(R.string.home_metric_running_hint),
                        icon = Icons.Default.PlayCircle,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = onNavigateToControlPanel
                    )
                }
            } else {
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomePinnedSpacesBlock(
    pinnedNames: List<String>,
    runningMap: Map<String, Boolean>,
    onOpen: (String) -> Unit,
    onBrowse: () -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = context.getString(R.string.home_pinned_spaces_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = context.getString(R.string.home_pinned_spaces_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (pinnedNames.isEmpty()) {
            Text(
                text = context.getString(R.string.home_pinned_spaces_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBrowse)
                    .padding(vertical = 4.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pinnedNames.forEach { name ->
                    val running = runningMap[name] == true
                    AssistChip(
                        onClick = { onOpen(name) },
                        label = {
                            Text(
                                text = if (running) {
                                    context.getString(R.string.home_pinned_space_running, name)
                                } else {
                                    name
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (running) Icons.Default.PlayCircle else Icons.Default.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRootfsBlock(
    homeLayout: HomeLayout,
    compactMetrics: Boolean,
    adaptive: AdaptiveMetrics,
    pinnedIds: Set<String>,
    onTogglePin: (String) -> Unit,
    onNavigateToRootfsRepo: () -> Unit
) {
    if (homeLayout == HomeLayout.FOCUS) return
    val context = LocalContext.current
    val orderedPresets = remember(pinnedIds) {
        val pinned = CuratedRootfsRepos.presets.filter { it.id in pinnedIds }
        val rest = CuratedRootfsRepos.presets.filter { it.id !in pinnedIds }
        pinned + rest
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(if (compactMetrics) 8.dp else 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = context.getString(R.string.home_rootfs_section),
                style = if (compactMetrics) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = context.getString(R.string.home_rootfs_subtitle_pin),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = if (compactMetrics) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        val useGrid = homeLayout == HomeLayout.GALLERY ||
            (homeLayout == HomeLayout.DASHBOARD && adaptive.widthClass != WidthClass.Compact)
        if (useGrid) {
            val cols = if (adaptive.widthClass == WidthClass.Expanded) 3 else 2
            orderedPresets.chunked(cols).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { repo ->
                        HomeRootfsRepoCard(
                            name = repo.name,
                            description = repo.description,
                            category = repo.category,
                            pinned = repo.id in pinnedIds,
                            onClick = onNavigateToRootfsRepo,
                            onLongClick = { onTogglePin(repo.id) },
                            compact = compactMetrics || homeLayout == HomeLayout.GALLERY,
                            modifier = Modifier.weight(1f),
                            stretch = true
                        )
                    }
                    repeat(cols - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Surface(
                onClick = onNavigateToRootfsRepo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compactMetrics) 56.dp else 64.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = context.getString(R.string.home_rootfs_browse_all),
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(orderedPresets, key = { it.id }) { repo ->
                    HomeRootfsRepoCard(
                        name = repo.name,
                        description = repo.description,
                        category = repo.category,
                        pinned = repo.id in pinnedIds,
                        onClick = onNavigateToRootfsRepo,
                        onLongClick = { onTogglePin(repo.id) },
                        compact = compactMetrics
                    )
                }
                item(key = "browse-all") {
                    val cardHeight = if (compactMetrics) 108.dp else 132.dp
                    Surface(
                        onClick = onNavigateToRootfsRepo,
                        modifier = Modifier
                            .width(148.dp)
                            .height(cardHeight),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
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
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = context.getString(R.string.home_rootfs_browse_all),
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAppearanceBlock(
    homeLayout: HomeLayout,
    adaptive: AdaptiveMetrics,
    showLook: Boolean,
    onShowLook: () -> Unit,
    prefsManager: PreferencesManager,
    themeState: ThemeState,
    orbitSlotsRaw: String,
    onOrbitSlotsChanged: (String) -> Unit,
    onOpenThemeEditor: () -> Unit
) {
    val context = LocalContext.current
    if (!showLook) {
        Surface(
            onClick = onShowLook,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = context.getString(R.string.home_layout_show_look),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = context.getString(R.string.home_appearance_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = context.getString(R.string.home_appearance_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        HomeLayoutPicker(
            selected = homeLayout,
            onSelected = { prefsManager.homeLayout = it.name }
        )

        Text(
            text = when (adaptive.widthClass) {
                WidthClass.Compact -> context.getString(R.string.home_adaptive_phone)
                WidthClass.Medium -> context.getString(R.string.home_adaptive_tablet)
                WidthClass.Expanded -> context.getString(R.string.home_adaptive_wide)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = context.getString(R.string.home_orbit_customize_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = context.getString(R.string.home_orbit_customize_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            val slots = HomeOrbitActionId.parseSlots(orbitSlotsRaw)
            slots.forEachIndexed { index, selectedId ->
                Text(
                    text = context.getString(R.string.home_orbit_slot_label, index + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(HomeOrbitCatalog.all, key = { "${index}-${it.id}" }) { def ->
                        val selected = def.id == selectedId
                        Surface(
                            onClick = {
                                prefsManager.setHomeOrbitSlot(index, def.id)
                                onOrbitSlotsChanged(prefsManager.homeOrbitSlots)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Text(
                                text = context.getString(def.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
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
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
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
            onClick = onOpenThemeEditor,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
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
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.theme_create_cta),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = context.getString(R.string.theme_create_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRootfsRepoCard(
    name: String,
    description: String,
    category: String,
    pinned: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    stretch: Boolean = false
) {
    val context = LocalContext.current
    val cardHeight = if (compact) 108.dp else 132.dp
    Surface(
        modifier = modifier
            .then(if (stretch) Modifier.fillMaxWidth() else Modifier.width(if (compact) 168.dp else 188.dp))
            .height(cardHeight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    Toast.makeText(
                        context,
                        if (!pinned) {
                            context.getString(R.string.home_rootfs_pinned_toast)
                        } else {
                            context.getString(R.string.home_rootfs_unpinned_toast)
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (pinned) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (pinned) {
                    Text(
                        text = context.getString(R.string.home_rootfs_pinned_chip),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
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
    // heightIn (not fixed height) — avoids clip when label/hint wrap on small widths / large fonts
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeLayoutPicker(
    selected: HomeLayout,
    onSelected: (HomeLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val options = listOf(
        Triple(
            HomeLayout.SIMPLE,
            R.string.home_layout_simple,
            R.string.home_layout_simple_desc
        ),
        Triple(
            HomeLayout.COMMAND,
            R.string.home_layout_command,
            R.string.home_layout_command_desc
        ),
        Triple(
            HomeLayout.COMPACT,
            R.string.home_layout_compact,
            R.string.home_layout_compact_desc
        ),
        Triple(
            HomeLayout.GALLERY,
            R.string.home_layout_gallery,
            R.string.home_layout_gallery_desc
        ),
        Triple(
            HomeLayout.FOCUS,
            R.string.home_layout_focus,
            R.string.home_layout_focus_desc
        ),
        Triple(
            HomeLayout.DASHBOARD,
            R.string.home_layout_dashboard,
            R.string.home_layout_dashboard_desc
        )
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = context.getString(R.string.home_layout_section),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.forEach { (layout, titleRes, descRes) ->
            val isSelected = selected == layout
            Surface(
                onClick = { onSelected(layout) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                CircleShape
                            )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = context.getString(descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCommandHero(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = context.getString(R.string.home_brand_kicker),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.2.sp
            )
            Text(
                text = context.getString(R.string.app_name),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.1).sp,
                    lineHeight = 40.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = context.getString(R.string.home_layout_tagline),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeOrbitAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.08f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeLivePulse(
    runningCount: Int,
    containerCount: Int,
    modifier: Modifier = Modifier
) {
    val active = runningCount > 0
    val infinite = rememberInfiniteTransition(label = "livePulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
                if (active) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = pulse * 0.45f
                            }
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = LocalContext.current.getString(R.string.home_live_pulse),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (active) {
                        LocalContext.current.getString(R.string.home_live_active, runningCount)
                    } else {
                        LocalContext.current.getString(R.string.home_live_idle)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = LocalContext.current.getString(
                    R.string.home_spaces_live,
                    containerCount,
                    runningCount
                ),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeCompactMetricRow(
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
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
private fun MainSideRail(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit
) {
    val context = LocalContext.current
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = Color.Transparent,
        header = {
            Text(
                text = "AB",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    ) {
        TabItem.entries.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(tab.icon, contentDescription = context.getString(tab.titleResId))
                },
                label = {
                    Text(
                        text = context.getString(tab.titleResId),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = SpaceGrotesk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
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

            // Floating dock — ink capsule, not Material NavigationBar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            ),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 5.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tabWidth = maxWidth / tabs.size
                    val offset by animateDpAsState(
                        targetValue = tabWidth * selectedIndex,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "IndicatorOffset"
                    )

                    Surface(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .offset(x = offset),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
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
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            label = "IconColor"
                        )

                        Surface(
                            onClick = { onTabSelected(tab) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            color = Color.Transparent,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (isSelected) 22.dp else 20.dp),
                                    tint = contentColor
                                )
                                if (isSelected) {
                                    Text(
                                        text = context.getString(tab.titleResId),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = SpaceGrotesk,
                                        color = contentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
