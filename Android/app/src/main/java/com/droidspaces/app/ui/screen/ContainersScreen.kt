package com.droidspaces.app.ui.screen

import com.droidspaces.app.ui.component.DsDialog
import com.droidspaces.app.ui.component.DsTextFieldDefaults

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidspaces.app.ui.component.DsSnackbarHost
import com.droidspaces.app.ui.util.FocusUtils
import com.droidspaces.app.ui.util.ProgressDialog
import com.droidspaces.app.ui.util.ErrorLogsDialog
import com.droidspaces.app.ui.util.LoadingIndicator
import com.droidspaces.app.ui.util.LoadingSize
import com.droidspaces.app.ui.util.showError
import com.droidspaces.app.ui.util.showSuccess
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidspaces.app.ui.viewmodel.SystemStatsViewModel
import com.droidspaces.app.util.ContainerInfo
import com.droidspaces.app.util.PreferencesManager
import com.droidspaces.app.util.FilePickerUtils
import com.droidspaces.app.ui.component.ContainerCard
import com.droidspaces.app.ui.component.ContainerCardActions
import com.droidspaces.app.ui.component.DialogFooterRow
import com.droidspaces.app.ui.component.TerminalDialog
import com.droidspaces.app.ui.component.EmptyState
import com.droidspaces.app.ui.component.ErrorState
import com.droidspaces.app.ui.component.RootUnavailableState
import com.droidspaces.app.ui.component.RootfsRepoSheet
import com.droidspaces.app.ui.viewmodel.ContainerViewModel
import com.droidspaces.app.ui.viewmodel.ContainerOperationsViewModel
import com.droidspaces.app.ui.viewmodel.UninstallState
import com.droidspaces.app.ui.viewmodel.SparseOperation
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.droidspaces.app.R
import com.droidspaces.app.util.AnimationUtils
import androidx.compose.ui.window.Dialog

private enum class ContainerStatusFilter(val id: String) {
    All("all"),
    Running("running"),
    Stopped("stopped")
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ContainersScreen(
    isBackendAvailable: Boolean,
    isRootAvailable: Boolean = true,
    onNavigateToInstallation: (Uri) -> Unit = {},
    onNavigateToEditContainer: (String) -> Unit = {},
    onNavigateToContainerDetails: (String) -> Unit = {},
    containerViewModel: ContainerViewModel,
    expandedContainerName: String?,
    onExpandedContainerNameChange: (String?) -> Unit,
    emptyStateBottomInset: Dp = 0.dp
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val systemStatsViewModel: SystemStatsViewModel = viewModel()
    val prefsManager = PreferencesManager.getInstance(context)

    val snackbarHostState = remember { SnackbarHostState() }

    // Container lifecycle/maintenance operations + their state live in the ViewModel.
    val opsViewModel: ContainerOperationsViewModel = viewModel()

    // UI-only state (dialog triggers / pending pickers).
    var showUninstallConfirmation by remember { mutableStateOf<ContainerInfo?>(null) }
    var pendingSparseOperation by remember { mutableStateOf<SparseOperation?>(null) }
    var pendingExportContainer by remember { mutableStateOf<ContainerInfo?>(null) }
    var showRepoSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(ContainerStatusFilter.All) }

    // File picker launcher - CreateDocument for saving the export archive
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        val container = pendingExportContainer
        pendingExportContainer = null
        if (uri != null && container != null) {
            scope.launch {
                opsViewModel.executeExport(container, uri, onError = { msg -> scope.showError(snackbarHostState, msg) })
            }
        }
    }

    // File picker launcher - accept all files, validate internally
    // We don't filter in the picker (MIME types are unreliable for tar.xz/tar.gz)
    // FilePickerUtils handles proper filename extraction from any URI type (including recent files)
    // and validates that the file is a .tar.xz or .tar.gz file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val (isValid, fileName) = FilePickerUtils.isValidTarball(context, uri)
                if (isValid && fileName != null) {
                    onNavigateToInstallation(uri)
                } else {
                    val errorMessage = if (fileName != null) {
                        context.getString(R.string.file_picker_error, fileName)
                    } else {
                        context.getString(R.string.file_picker_error_unknown)
                    }
                    scope.showError(snackbarHostState, errorMessage)
                }
            }
        }
    }

    // Get containers from ViewModel - single source of truth (KernelSU pattern)
    val containers = containerViewModel.containerList

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Show root unavailable state first, then backend unavailable, then content
        when {
            !isRootAvailable -> {
                RootUnavailableState(modifier = Modifier.padding(bottom = emptyStateBottomInset))
            }
            !isBackendAvailable -> {
                ErrorState(modifier = Modifier.padding(bottom = emptyStateBottomInset))
            }
            containers.isEmpty() -> {
                if (containerViewModel.isRefreshing) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = emptyStateBottomInset),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(size = LoadingSize.Large)
                    }
                } else {
                    EmptyState(
                        icon = Icons.Default.Storage,
                        title = context.getString(R.string.no_containers_installed),
                        description = context.getString(R.string.install_container_description),
                        // Reserve the floating tab bar's space so the centered
                        // content sits in the visible region, not behind the bar.
                        modifier = Modifier.padding(bottom = emptyStateBottomInset)
                    )
                }
            }
            else -> {
                val filteredContainers = remember(containers, searchQuery, statusFilter) {
                    containers.filter { container ->
                        val matchesQuery = searchQuery.isBlank() ||
                            container.name.contains(searchQuery, ignoreCase = true) ||
                            container.hostname.contains(searchQuery, ignoreCase = true)
                        val matchesStatus = when (statusFilter) {
                            ContainerStatusFilter.All -> true
                            ContainerStatusFilter.Running -> container.isRunning
                            ContainerStatusFilter.Stopped -> !container.isRunning
                        }
                        matchesQuery && matchesStatus
                    }
                }
                val filterCounts = remember(containers) {
                    mapOf(
                        ContainerStatusFilter.All.id to containers.size,
                        ContainerStatusFilter.Running.id to containers.count { it.isRunning },
                        ContainerStatusFilter.Stopped.id to containers.count { !it.isRunning }
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    ContainerSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                    ContainerFilterChipsRow(
                        selectedFilter = statusFilter,
                        counts = filterCounts,
                        onFilterSelected = { statusFilter = it }
                    )

                    if (filteredContainers.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = context.getString(R.string.no_containers_match),
                            description = context.getString(R.string.no_containers_match_description),
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = emptyStateBottomInset)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onExpandedContainerNameChange(null) }
                                )
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredContainers, key = { it.name }) { container ->
                                val isRunning = opsViewModel.runningOperationContainer == container.name
                                var appeared by remember(container.name) { mutableStateOf(false) }
                                LaunchedEffect(container.name) { appeared = true }
                                val enterAlpha by animateFloatAsState(
                                    targetValue = if (appeared) 1f else 0f,
                                    animationSpec = AnimationUtils.cardFadeSpec(),
                                    label = "cardEnterAlpha"
                                )

                                ContainerCard(
                                    modifier = Modifier
                                        .alpha(enterAlpha)
                                        .animateItemPlacement(AnimationUtils.mediumSpec()),
                                    container = container,
                                    isOperationRunning = isRunning,
                                    isExpanded = expandedContainerName == container.name,
                                    actions = ContainerCardActions(
                                    onToggleExpand = {
                                        onExpandedContainerNameChange(if (expandedContainerName == container.name) null else container.name)
                                    },
                                     onShowLogs = {
                                        opsViewModel.showLogViewerFor = container.name
                                    },
                                    onStart = {
                                        scope.launch {
                                            opsViewModel.executeOperation(
                                                container, "start",
                                                onRefresh = { containerViewModel.refresh() },
                                                onClearUsage = { systemStatsViewModel.clearContainerUsage(it) },
                                                onFailureSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long) } }
                                            )
                                        }
                                    },
                                    onStop = {
                                        scope.launch {
                                            opsViewModel.executeOperation(
                                                container, "stop",
                                                onRefresh = { containerViewModel.refresh() },
                                                onClearUsage = { systemStatsViewModel.clearContainerUsage(it) },
                                                onFailureSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long) } }
                                            )
                                        }
                                    },
                                    onRestart = {
                                        scope.launch {
                                            opsViewModel.executeOperation(
                                                container, "restart",
                                                onRefresh = { containerViewModel.refresh() },
                                                onClearUsage = { systemStatsViewModel.clearContainerUsage(it) },
                                                onFailureSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Long) } }
                                            )
                                        }
                                    },
                                    onEdit = {
                                        onExpandedContainerNameChange(null)
                                        onNavigateToEditContainer(container.name)
                                    },
                                    onEnter = {
                                        onNavigateToContainerDetails(container.name)
                                    },
                                    onUninstall = {
                                        onExpandedContainerNameChange(null)
                                        showUninstallConfirmation = container
                                    },
                                    onMigrate = {
                                        onExpandedContainerNameChange(null)
                                        pendingSparseOperation = SparseOperation.Migrate(container)
                                    },
                                    onResize = {
                                        onExpandedContainerNameChange(null)
                                        pendingSparseOperation = SparseOperation.Resize(container)
                                    },
                                    onExport = {
                                        onExpandedContainerNameChange(null)
                                        // Generate filename: <name>_yyyyMMdd_HHmmss.tar.gz
                                        val timestamp = java.text.SimpleDateFormat(
                                            "yyyyMMdd_HHmmss",
                                            java.util.Locale.US
                                        ).format(java.util.Date())
                                        val fileName = "${container.name}_${timestamp}.tar.gz"
                                        pendingExportContainer = container
                                        exportFileLauncher.launch(fileName)
                                    }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB LAYER (Above everything, below dialogs)
        if (isBackendAvailable && isRootAvailable) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Small secondary FAB: browse online repo (icon only)
                SmallFloatingActionButton(
                    onClick = { showRepoSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = context.getString(R.string.repo_fab_label),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Primary FAB: install local file
                ExtendedFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = context.getString(R.string.install),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        // Repo bottom sheet
        if (showRepoSheet) {
            val orientation = LocalConfiguration.current.orientation
            key(orientation) {
                RootfsRepoSheet(
                    onDismiss = { showRepoSheet = false },
                    onInstall = { uri -> onNavigateToInstallation(uri) }
                )
            }
        }

        // SNACKBAR LAYER (Highest Z-index in the root Box)
        DsSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        )

        // Log viewer dialog - console stays open, user must close manually
        opsViewModel.showLogViewerFor?.let { containerName ->
            // Load logs from memory first, fallback to cache if empty
            val memoryLogs = opsViewModel.containerLogs[containerName]?.toList() ?: emptyList()
            val cachedLogs = if (memoryLogs.isEmpty()) {
                prefsManager.loadContainerLogs(containerName)
            } else {
                emptyList()
            }
            val logs = memoryLogs.ifEmpty { cachedLogs }
            val isBlocking = opsViewModel.runningOperationContainer == containerName // Blocking when operation is running
            TerminalDialog(
                title = context.getString(R.string.logs_title, containerName),
                logs = logs,
                onDismiss = {
                    opsViewModel.showLogViewerFor = null
                    // Refresh container status when console is closed (KernelSU pattern)
                    containerViewModel.refresh()
                },
                onClear = {
                    opsViewModel.clearLogsBuffer(containerName)
                },
                isBlocking = isBlocking // Block dismissal when operation is running
            )
        }

        // Uninstall confirmation dialog
        showUninstallConfirmation?.let { container ->
            UninstallConfirmationDialog(
                containerName = container.name,
                onConfirm = {
                    showUninstallConfirmation = null
                    scope.launch {
                        opsViewModel.executeUninstall(
                            container,
                            onError = { msg -> scope.showError(snackbarHostState, msg) },
                            onSuccess = { msg -> scope.showSuccess(snackbarHostState, msg) },
                            onRefresh = { containerViewModel.refresh() }
                        )
                    }
                },
                onDismiss = {
                    showUninstallConfirmation = null
                }
            )
        }

        // Uninstall progress dialog
        (opsViewModel.uninstallState as? UninstallState.InProgress)?.let { state ->
            ProgressDialog(
                message = state.message
            )
        }

        // Uninstall logs dialog (only on failure)
        opsViewModel.uninstallLogsDialog?.let { logs ->
            ErrorLogsDialog(
                logs = logs,
                onDismiss = { opsViewModel.dismissUninstallLogs() }
            )
        }

        // Sparse operation size dialog
        pendingSparseOperation?.let { op ->
            val container = when (op) {
                is SparseOperation.Migrate -> op.container
                is SparseOperation.Resize -> op.container
            }

            SparseSizeDialog(
                title = context.getString(
                    if (op is SparseOperation.Migrate) R.string.migrate_dialog_title
                    else R.string.resize_dialog_title
                ),
                message = context.getString(
                    if (op is SparseOperation.Migrate) R.string.migrate_dialog_message
                    else R.string.resize_dialog_message,
                    container.name
                ),
                initialSize = container.sparseImageSizeGB ?: 8,
                // Only a resize can be a no-op: a migrate has no image yet, so
                // picking the default size there is a legitimate choice.
                currentSize = if (op is SparseOperation.Resize) container.sparseImageSizeGB else null,
                onConfirm = { size ->
                    pendingSparseOperation = null
                    scope.launch {
                        opsViewModel.executeSparseOperation(op, size, onRefresh = { containerViewModel.refresh() })
                    }
                },
                onDismiss = { pendingSparseOperation = null }
            )
        }
    }
}

@Composable
private fun SparseSizeDialog(
    title: String,
    message: String,
    initialSize: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    currentSize: Int? = null
) {
    val context = LocalContext.current
    var sizeText by remember { mutableStateOf(initialSize.toString()) }
    val size = sizeText.toIntOrNull()
    val isOutOfRange = size == null || size !in 4..512
    // The field opens pre-filled with the image's existing size, so confirming
    // straight away would run a resize that resizes nothing.
    val isSameSize = currentSize != null && size == currentSize
    val isValid = !isOutOfRange && !isSameSize

    DsDialog(
        onDismiss = onDismiss,
        modifier = Modifier.imePadding(),
        footer = {
            DialogFooterRow(
                dismissLabel = context.getString(R.string.cancel),
                confirmLabel = context.getString(R.string.continue_button),
                onDismiss = onDismiss,
                onConfirm = { size?.let { onConfirm(it) } },
                confirmEnabled = isValid
            )
        }
    ) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = sizeText,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) sizeText = it },
                label = { Text(context.getString(R.string.size_gb)) },
                placeholder = { Text(context.getString(R.string.size_range_4_512_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = DsTextFieldDefaults.colors(),
                isError = !isValid && sizeText.isNotEmpty(),
                supportingText = {
                    if (sizeText.isNotEmpty()) {
                        if (isOutOfRange) {
                            Text(context.getString(R.string.enter_size_between_4_512_gb))
                        } else if (isSameSize) {
                            Text(context.getString(R.string.resize_same_as_current, currentSize))
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )
    }
    }


@Composable
private fun UninstallConfirmationDialog(
    containerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var confirmText by remember { mutableStateOf("") }
    val isConfirmed = confirmText == containerName

    DsDialog(
        onDismiss = onDismiss,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
        footer = {
            DialogFooterRow(
                dismissLabel = context.getString(R.string.cancel),
                confirmLabel = context.getString(R.string.uninstall),
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                confirmEnabled = isConfirmed,
                destructive = true
            )
        }
    ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Warning, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp)
                )
                Text(
                    text = context.getString(R.string.uninstall_container_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = buildAnnotatedString {
                    val template = context.getString(R.string.uninstall_container_message)
                    val parts = template.split("%1\$s")
                    append(parts.getOrElse(0) { "" })
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(containerName) }
                    append(parts.getOrElse(1) { "" })
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = context.getString(R.string.type_container_name_to_confirm),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(containerName) },
                    singleLine = true,
                    isError = confirmText.isNotEmpty() && !isConfirmed,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                )
            }
    }
    }


@Composable
private fun ContainerSearchBar(query: String, onQueryChange: (String) -> Unit) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        animationSpec = AnimationUtils.fastSpec(),
        label = "containerSearchBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
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
                    context.getString(R.string.search_containers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isFocused) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
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

@Composable
private fun ContainerFilterChipsRow(
    selectedFilter: ContainerStatusFilter,
    counts: Map<String, Int>,
    onFilterSelected: (ContainerStatusFilter) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val filters = listOf(
        ContainerStatusFilter.All to R.string.all_legend,
        ContainerStatusFilter.Running to R.string.running,
        ContainerStatusFilter.Stopped to R.string.stopped
    )

    LaunchedEffect(selectedFilter) {
        val idx = filters.indexOfFirst { it.first == selectedFilter }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(filters) { (filter, labelRes) ->
            val count = counts[filter.id] ?: 0
            val isSelected = selectedFilter == filter
            val dotColor = when (filter) {
                ContainerStatusFilter.All -> null
                ContainerStatusFilter.Running -> MaterialTheme.colorScheme.primary
                ContainerStatusFilter.Stopped -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            }
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (dotColor != null) {
                            Surface(
                                modifier = Modifier.size(6.dp),
                                shape = CircleShape,
                                color = dotColor
                            ) {}
                        }
                        Text(
                            "${context.getString(labelRes)} ($count)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selected = isSelected,
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
