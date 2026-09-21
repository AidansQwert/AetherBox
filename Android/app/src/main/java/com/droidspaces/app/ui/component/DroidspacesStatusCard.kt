package com.droidspaces.app.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidspaces.app.R
import com.droidspaces.app.ui.theme.JetBrainsMono
import com.droidspaces.app.util.AnimationUtils
import com.droidspaces.app.util.ApkInstallStatus
import com.droidspaces.app.util.ApkUpdateInstaller
import com.droidspaces.app.util.AppUpdateInfo
import com.droidspaces.app.util.SystemInfoManager
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class DroidspacesStatus {
    Working,
    UpdateAvailable,
    NotInstalled,
    Unsupported,
    Corrupted,
    ModuleMissing
}

@Composable
fun DroidspacesStatusCard(
    status: DroidspacesStatus,
    version: String? = null,
    isChecking: Boolean = false,
    isRootAvailable: Boolean = true,
    refreshTrigger: Int = 0,
    appUpdate: AppUpdateInfo? = null,
    onDismissAppUpdate: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isWorking = isRootAvailable && status == DroidspacesStatus.Working

    var droidspacesVersion by remember {
        mutableStateOf(version ?: SystemInfoManager.getCachedDroidspacesVersion(context))
    }
    var backendMode by remember {
        mutableStateOf(if (isWorking) SystemInfoManager.getCachedBackendMode(context) else null)
    }

    LaunchedEffect(status, isRootAvailable, refreshTrigger) {
        if (isWorking) {
            droidspacesVersion = SystemInfoManager.getDroidspacesVersion(context)
            backendMode = SystemInfoManager.getBackendMode(context)
        } else {
            backendMode = null
        }
    }

    val accentColor = when {
        isWorking -> MaterialTheme.colorScheme.primary
        status == DroidspacesStatus.UpdateAvailable -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val isError = !isWorking

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surfaceContainer

    val pulse = rememberInfiniteTransition(label = "heroPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationUtils.DURATION_SLOW * 8,
                easing = AnimationUtils.STANDARD_EASING
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val orbShift by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = AnimationUtils.STANDARD_EASING),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb"
    )

    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.22f),
                            surface,
                            secondary.copy(alpha = 0.14f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 700f)
                    )
                )
        ) {
            // Soft atmospheric orbs — brand presence without clutter.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (28 + orbShift).dp, y = (-36).dp)
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = glow),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-24).dp, y = (28 - orbShift / 2).dp)
                    .size(110.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                secondary.copy(alpha = glow * 0.7f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = accentColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.BlurOn,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }

                        val beaconPulse = rememberInfiniteTransition(label = "beacon")
                        val pulsedAlpha by beaconPulse.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = AnimationUtils.DURATION_SLOW * 4,
                                    easing = AnimationUtils.STANDARD_EASING
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "beaconAlpha"
                        )
                        val beaconAlpha = if (isWorking && !isChecking) pulsedAlpha else 1f

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .alpha(beaconAlpha),
                                    shape = CircleShape,
                                    color = if (isChecking) {
                                        MaterialTheme.colorScheme.outline
                                    } else {
                                        accentColor
                                    }
                                ) {}
                                Text(
                                    text = when {
                                        !isRootAvailable -> context.getString(R.string.root_unavailable)
                                        isChecking -> context.getString(R.string.backend_checking)
                                        isWorking -> context.getString(R.string.backend_installed)
                                        status == DroidspacesStatus.UpdateAvailable ->
                                            context.getString(R.string.backend_update_available)
                                        else -> context.getString(R.string.backend_attention_required)
                                    }.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = context.getString(R.string.home_hero_caption),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (isWorking && backendMode != null) {
                        StatusPill(label = backendMode!!.uppercase(), color = accentColor)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.8).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            isWorking || status == DroidspacesStatus.UpdateAvailable ->
                                context.getString(R.string.home_hero_ready)
                            !isRootAvailable -> context.getString(R.string.root_unavailable)
                            status == DroidspacesStatus.NotInstalled ->
                                context.getString(R.string.backend_not_installed)
                            else -> context.getString(R.string.backend_corrupted)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    if (isWorking) {
                        Text(
                            text = "${context.getString(R.string.version)} ${droidspacesVersion ?: "—"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (isError) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !isRootAvailable -> context.getString(R.string.grant_root_message)
                                    status == DroidspacesStatus.UpdateAvailable ->
                                        context.getString(R.string.update_available_message)
                                    else -> context.getString(R.string.tap_to_fix_system)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = appUpdate != null,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    val update = appUpdate ?: return@AnimatedVisibility
                    val updateColor = MaterialTheme.colorScheme.tertiary
                    var downloading by remember(update.version) { mutableStateOf(false) }
                    var downloadProgress by remember(update.version) { mutableStateOf(0) }
                    var downloadError by remember(update.version) { mutableStateOf<String?>(null) }
                    val scope = rememberCoroutineScope()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = updateColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, updateColor.copy(alpha = 0.22f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = updateColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = context.getString(
                                        R.string.app_update_message,
                                        update.displayVersion
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = updateColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (downloadError != null) {
                                Text(
                                    text = downloadError!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (downloading) {
                                Text(
                                    text = context.getString(
                                        R.string.app_update_downloading_progress,
                                        downloadProgress
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = updateColor
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        ApkUpdateInstaller.openReleasePage(context, update.releaseUrl)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = updateColor.copy(alpha = 0.16f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = updateColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = context.getString(R.string.app_update_notes),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = updateColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Surface(
                                    onClick = download@{
                                        val apkUrl = update.apkUrl
                                        if (apkUrl.isNullOrBlank()) {
                                            ApkUpdateInstaller.openReleasePage(context, update.releaseUrl)
                                            return@download
                                        }
                                        if (downloading) return@download
                                        downloading = true
                                        downloadError = null
                                        scope.launch {
                                            ApkUpdateInstaller.downloadAndInstall(
                                                context,
                                                apkUrl,
                                                update.displayVersion
                                            ).collect { status ->
                                                when (status) {
                                                    is ApkInstallStatus.Progress ->
                                                        downloadProgress = status.percent
                                                    is ApkInstallStatus.Ready ->
                                                        downloading = false
                                                    is ApkInstallStatus.Failed -> {
                                                        downloading = false
                                                        downloadError = status.reason
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = updateColor.copy(alpha = 0.28f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = context.getString(R.string.app_update_download),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = updateColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                            .fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Surface(
                                    onClick = onDismissAppUpdate,
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = context.getString(R.string.app_update_later),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = updateColor.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }


                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )

                Text(
                    text = context.getString(R.string.welcome_tagline),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
