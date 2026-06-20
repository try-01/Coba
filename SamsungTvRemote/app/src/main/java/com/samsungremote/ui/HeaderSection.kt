package com.samsungremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.samsungremote.TvConnectionState

@Composable
fun ConnectionHeader(
    connectionState: TvConnectionState,
    showConnectionSheet: Boolean,
    onToggleConnectionSheet: () -> Unit,
    onWakeOnLan: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is TvConnectionState.Connected -> RemoteColors.OkGreen
            is TvConnectionState.Connecting,
            is TvConnectionState.Discovering -> RemoteColors.Amber
            else -> RemoteColors.PowerRed
        },
        label = "statusColor"
    )

    val statusLabel = when (connectionState) {
        is TvConnectionState.Connected -> {
            val name = connectionState.deviceName.ifBlank { connectionState.ip }
            "Connected · $name"
        }
        is TvConnectionState.Connecting -> "Connecting…"
        is TvConnectionState.Discovering -> "Scanning…"
        is TvConnectionState.Error -> connectionState.throwable.localizedMessage ?: "Connection Error"
        is TvConnectionState.Disconnected -> {
            connectionState.reason.ifBlank { "Disconnected" }
        }
        TvConnectionState.Idle -> "Disconnected"
    }

    val showProgress = connectionState is TvConnectionState.Connecting ||
            connectionState is TvConnectionState.Discovering

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(RemoteColors.BackgroundDeep.copy(alpha = 0.5f))
            .clickable(onClick = onToggleConnectionSheet)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TV·SYS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.em,
                color = RemoteColors.NeonCyan.copy(alpha = 0.7f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.06.em,
                        color = RemoteColors.OnSurfaceDim,
                        maxLines = if (connectionState is TvConnectionState.Error) 8 else 1
                    )
                }
                RemoteButton(
                    onClick = onSettingsClick,
                    icon = Icons.Filled.Settings,
                    size = 30.dp,
                    shape = RoundedCornerShape(8.dp),
                    tint = RemoteColors.OnSurfaceDim,
                    contentDescription = "Settings",
                    hapticEnabled = false
                )
            }
        }
        if (showProgress) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = RemoteColors.NeonCyan,
                trackColor = RemoteColors.KeyBorder
            )
        }
    }
}
