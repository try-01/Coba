package com.samsungremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsungremote.TvConnectionState

@Composable
fun ConnectionHeader(
    connectionState: TvConnectionState,
    showConnectionSheet: Boolean,
    onToggleConnectionSheet: () -> Unit,
    onWakeOnLan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is TvConnectionState.Connected -> RemoteColors.NeonGreen
            is TvConnectionState.Connecting,
            is TvConnectionState.Discovering -> RemoteColors.Amber
            else -> RemoteColors.ErrorRed
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
        is TvConnectionState.Error -> "Error: ${connectionState.throwable.localizedMessage ?: "Unknown"}"
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
            .background(RemoteColors.Surface)
            .clickable(onClick = onToggleConnectionSheet)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = statusLabel,
                        color = RemoteColors.OnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "Tap to change",
                        color = RemoteColors.OnSurfaceDim,
                        fontSize = 10.sp
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // WoL button (only show when disconnected)
                if (connectionState !is TvConnectionState.Connected) {
                    RemoteButton(
                        onClick = onWakeOnLan,
                        icon = Icons.Filled.Bolt,
                        size = 38.dp,
                        shape = RoundedCornerShape(10.dp),
                        tint = RemoteColors.Amber,
                        contentDescription = "Wake on LAN",
                        hapticEnabled = true
                    )
                }

                // Connection details button
                RemoteButton(
                    onClick = onToggleConnectionSheet,
                    icon = Icons.Filled.Sensors,
                    size = 38.dp,
                    shape = RoundedCornerShape(10.dp),
                    tint = RemoteColors.NeonCyan,
                    contentDescription = "Connection settings",
                    hapticEnabled = false
                )
            }
        }

        // Connecting progress bar
        if (showProgress) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = RemoteColors.NeonCyan,
                trackColor = RemoteColors.ButtonMid,
            )
        }
    }
}
