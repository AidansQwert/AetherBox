package com.droidspaces.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidspaces.app.R
import com.droidspaces.app.ui.component.DsSnackbarHost
import com.droidspaces.app.ui.component.EmptyState
import com.droidspaces.app.ui.component.ErrorState
import com.droidspaces.app.ui.component.RootUnavailableState
import com.droidspaces.app.ui.component.RunningContainerCard
import com.droidspaces.app.ui.theme.WidthClass
import com.droidspaces.app.ui.theme.rememberAdaptiveMetrics
import com.droidspaces.app.ui.viewmodel.ContainerViewModel
import com.droidspaces.app.ui.viewmodel.SystemStatsViewModel
import com.droidspaces.app.util.ThermalMonitor
import com.droidspaces.app.util.ThermalSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Control Panel — running containers + host thermal.
 *
 * Pull-to-refresh is provided by the parent tab wrapper.
 */
@Composable
fun ControlPanelScreen(
    isBackendAvailable: Boolean,
    isRootAvailable: Boolean = true,
    containerViewModel: ContainerViewModel,
    onNavigateToContainerDetails: (String) -> Unit = {},
    onNavigateToTerminal: (String) -> Unit = {},
    emptyStateBottomInset: Dp = 0.dp,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val systemStatsViewModel: SystemStatsViewModel = viewModel()
    val adaptive = rememberAdaptiveMetrics()

    val runningContainers = containerViewModel.containerList.filter { it.isRunning }
    val lifecycleOwner = LocalLifecycleOwner.current

    var thermal by remember { mutableStateOf(ThermalMonitor.snapshot(context)) }

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                thermal = ThermalMonitor.snapshot(context)
                delay(3_000)
            }
        }
    }

    LaunchedEffect(runningContainers) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            systemStatsViewModel.monitorContainers(runningContainers)
        }
    }

    val containerUsageMap = systemStatsViewModel.containerUsageMap
    val useTwoCol = adaptive.widthClass != WidthClass.Compact && runningContainers.size > 1

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !isRootAvailable -> {
                RootUnavailableState(modifier = Modifier.padding(bottom = emptyStateBottomInset))
            }
            !isBackendAvailable -> {
                ErrorState(modifier = Modifier.padding(bottom = emptyStateBottomInset))
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = if (adaptive.useRail) 28.dp else 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 8 }
                    ) {
                        PanelHeader(
                            runningCount = runningContainers.size,
                            thermal = thermal
                        )
                    }

                    if (runningContainers.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Dashboard,
                            title = context.getString(R.string.no_containers_running),
                            description = context.getString(R.string.start_container_first),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        )
                    } else if (useTwoCol) {
                        runningContainers.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { container ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        RunningContainerCard(
                                            container = container,
                                            onEnter = {
                                                onNavigateToContainerDetails(container.name)
                                            },
                                            onTerminalClick = {
                                                onNavigateToTerminal(container.name)
                                            },
                                            osInfo = containerUsageMap[container.name],
                                        )
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        runningContainers.forEach { container ->
                            RunningContainerCard(
                                container = container,
                                onEnter = {
                                    onNavigateToContainerDetails(container.name)
                                },
                                onTerminalClick = {
                                    onNavigateToTerminal(container.name)
                                },
                                osInfo = containerUsageMap[container.name],
                            )
                        }
                    }
                }
            }
        }

        DsSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PanelHeader(runningCount: Int, thermal: ThermalSnapshot) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val accent = thermalAccent(thermal.statusLevel, scheme.primary)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = context.getString(R.string.panel_live_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = context.getString(R.string.panel_running_count, runningCount),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )

        Surface(
            color = scheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = context.getString(R.string.panel_thermal_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = thermal.summary,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ThermalMetric(
                        label = context.getString(R.string.panel_thermal_status),
                        value = thermal.statusLabel,
                        mod = Modifier.weight(1f)
                    )
                    ThermalMetric(
                        label = context.getString(R.string.panel_thermal_battery),
                        value = thermal.batteryCelsius?.let { String.format("%.1f°C", it) }
                            ?: thermal.zoneCelsius?.let { String.format("%.1f°C", it) }
                            ?: "—",
                        mod = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThermalMetric(
    label: String,
    value: String,
    mod: Modifier = Modifier
) {
    Column(modifier = mod) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun thermalAccent(level: Int, fallback: Color): Color = when (level) {
    0, 1 -> fallback
    2 -> Color(0xFFE6A817)
    3, 4 -> Color(0xFFE85D4C)
    else -> Color(0xFFD32F2F)
}
