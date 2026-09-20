package com.droidspaces.app.ui.component

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidspaces.app.R
import com.droidspaces.app.ui.theme.JetBrainsMono
import com.droidspaces.app.util.PreferencesManager
import com.droidspaces.app.util.SystemInfoManager

@Composable
fun SystemInfoCard(
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    var selinuxStatus by remember {
        mutableStateOf(
            SystemInfoManager.cachedSelinuxStatus
                ?: PreferencesManager.getInstance(context).cachedSelinuxStatus
                ?: context.getString(R.string.loading)
        )
    }

    val kernelVersion = SystemInfoManager.kernelVersion
    val architecture = SystemInfoManager.architecture
    val androidVersion = SystemInfoManager.androidVersion

    LaunchedEffect(refreshTrigger) {
        selinuxStatus = SystemInfoManager.refreshSELinuxStatus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = context.getString(R.string.home_system_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            SystemInfoItem(
                icon = Icons.Default.DeveloperBoard,
                title = context.getString(R.string.kernel),
                description = kernelVersion
            )
            SoftDivider()
            SystemInfoItem(
                icon = Icons.Default.Security,
                title = context.getString(R.string.selinux_status),
                description = selinuxStatus
            )
            SoftDivider()
            SystemInfoItem(
                icon = Icons.Default.Android,
                title = context.getString(R.string.android_version),
                description = androidVersion
            )
            SoftDivider()
            SystemInfoItem(
                icon = Icons.Default.BuildCircle,
                title = context.getString(R.string.architecture),
                description = architecture
            )
            SoftDivider()
            SystemInfoItem(
                icon = Icons.Default.Memory,
                title = context.getString(R.string.supported_abis),
                description = Build.SUPPORTED_ABIS.joinToString(", ")
            )
        }
    }
}

@Composable
private fun SoftDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
    )
}

@Composable
private fun SystemInfoItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
