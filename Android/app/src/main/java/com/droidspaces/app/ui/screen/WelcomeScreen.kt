package com.droidspaces.app.ui.screen
import androidx.compose.ui.graphics.Color

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidspaces.app.R
import com.droidspaces.app.ui.component.PrimaryActionBottomBar
import com.droidspaces.app.util.AnimationUtils
import kotlinx.coroutines.delay

private data class ShowcaseCard(val icon: ImageVector, val titleRes: Int, val descRes: Int)

@Composable
fun WelcomeScreen(onNavigateToRootCheck: () -> Unit) {
    val context = LocalContext.current

    var iconVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var cardsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(40); iconVisible = true
        delay(100); titleVisible = true
        delay(120); cardsVisible = true
    }

    val iconAlpha by animateFloatAsState(if (iconVisible) 1f else 0f, AnimationUtils.fadeInSpec(), label = "iconA")
    val iconScale by animateFloatAsState(
        if (iconVisible) 1f else 0.82f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "iconS"
    )
    val titleAlpha by animateFloatAsState(if (titleVisible) 1f else 0f, AnimationUtils.fadeInSpec(), label = "titleA")
    val titleOffset by animateFloatAsState(
        if (titleVisible) 0f else 18f,
        AnimationUtils.mediumSpec(),
        label = "titleY"
    )
    val cardsAlpha by animateFloatAsState(if (cardsVisible) 1f else 0f, AnimationUtils.fadeInSpec(), label = "cardsA")

    val pulse = rememberInfiniteTransition(label = "heroPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val cards = listOf(
        ShowcaseCard(Icons.Default.Terminal, R.string.feat_containers_title, R.string.feat_containers_desc),
        ShowcaseCard(Icons.Default.Speed, R.string.feat_overhead_title, R.string.feat_overhead_desc),
        ShowcaseCard(Icons.Default.Lock, R.string.feat_unkillable_title, R.string.feat_unkillable_desc),
        ShowcaseCard(Icons.Default.Settings, R.string.feat_init_title, R.string.feat_init_desc),
        ShowcaseCard(Icons.Default.Shield, R.string.feat_isolation_title, R.string.feat_isolation_desc),
        ShowcaseCard(Icons.Default.Usb, R.string.feat_hardware_title, R.string.feat_hardware_desc),
        ShowcaseCard(Icons.Default.VpnKey, R.string.feat_privileged_title, R.string.feat_privileged_desc),
        ShowcaseCard(Icons.Default.PowerSettingsNew, R.string.feat_autoboot_title, R.string.feat_autoboot_desc),
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            PrimaryActionBottomBar(
                label = context.getString(R.string.get_started),
                icon = Icons.Default.RocketLaunch,
                onClick = onNavigateToRootCheck,
                dividerAlpha = 0.4f,
                horizontalPadding = 20.dp,
                labelFontSize = 16.sp
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .alpha(iconAlpha)
                    .scale(iconScale)
                    .size(168.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(150.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = glow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    tonalElevation = 0.dp
                ) {}
                Icon(
                    painter = painterResource(id = R.drawable.ic_tux),
                    contentDescription = null,
                    modifier = Modifier.size(118.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .alpha(titleAlpha)
                    .graphicsLayer { translationY = titleOffset },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = context.getString(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = context.getString(R.string.welcome_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth().alpha(cardsAlpha),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cards.forEachIndexed { index, card ->
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(cardsVisible) {
                        if (cardsVisible) {
                            delay(index * 45L)
                            shown = true
                        }
                    }
                    val cardAlpha by animateFloatAsState(
                        if (shown) 1f else 0f,
                        AnimationUtils.cardFadeSpec(),
                        label = "cardA$index"
                    )
                    val cardY by animateFloatAsState(
                        if (shown) 0f else 14f,
                        AnimationUtils.mediumSpec(),
                        label = "cardY$index"
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(cardAlpha)
                            .graphicsLayer { translationY = cardY },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                tonalElevation = 0.dp
                            ) {
                                Icon(
                                    imageVector = card.icon,
                                    contentDescription = null,
                                    modifier = Modifier.padding(10.dp).size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = context.getString(card.titleRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(card.descRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
