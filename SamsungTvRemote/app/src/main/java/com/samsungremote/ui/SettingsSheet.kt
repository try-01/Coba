package com.samsungremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsungremote.TvConnectionState

@Composable
fun SettingsSheetContent(
    connectionState: TvConnectionState,
    hapticEnabled: Boolean,
    buttonScale: Float,
    serviceEnabled: Boolean,
    onHapticToggle: (Boolean) -> Unit,
    onScaleChange: (Float) -> Unit,
    onServiceToggle: () -> Unit,
    onDisconnect: () -> Unit,
    onShutdownServer: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is TvConnectionState.Connected

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Title ────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = RemoteColors.NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = RemoteColors.OnSurface
            )
        }

        HorizontalDivider(color = RemoteColors.ButtonMid)

        // ── Haptic toggle ────────────────────────────────────────
        SettingsRow(
            icon = Icons.Filled.TouchApp,
            label = "Haptic Feedback",
            subtitle = "Vibrate on button press"
        ) {
            Switch(
                checked = hapticEnabled,
                onCheckedChange = onHapticToggle,
                colors = switchColors()
            )
        }

        // ── Button scale ─────────────────────────────────────────
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = null,
                    tint = RemoteColors.OnSurfaceDim,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Button Scale",
                    color = RemoteColors.OnSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("0.5\u00D7", color = RemoteColors.OnSurfaceDim, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = buttonScale,
                    onValueChange = onScaleChange,
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = sliderColors()
                )
                Text("2.0\u00D7", color = RemoteColors.OnSurfaceDim, style = MaterialTheme.typography.labelSmall)
            }
        }

        // ── Service toggle ───────────────────────────────────────
        SettingsRow(
            icon = Icons.Filled.PowerSettingsNew,
            label = "Remote Service",
            subtitle = if (serviceEnabled) "Active \u2014 scan & connect enabled" else "Disabled \u2014 saves battery"
        ) {
            Switch(
                checked = serviceEnabled,
                onCheckedChange = { onServiceToggle() },
                colors = switchColors()
            )
        }

        HorizontalDivider(color = RemoteColors.ButtonMid)

        // ── Connection controls ──────────────────────────────────

        if (isConnected) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RemoteColors.Amber.copy(alpha = 0.15f),
                    contentColor = RemoteColors.Amber
                )
            ) {
                Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Disconnect from TV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Button(
            onClick = onShutdownServer,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RemoteColors.Amber.copy(alpha = 0.15f),
                contentColor = RemoteColors.Amber
            )
        ) {
            Icon(Icons.Filled.PowerOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Shut Down Server", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(Modifier.height(4.dp))

        // ── Deep exit button ─────────────────────────────────────
        Button(
            onClick = onExitApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RemoteColors.ErrorRed.copy(alpha = 0.15f),
                contentColor = RemoteColors.ErrorRed
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Dangerous,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "EXIT APP \u2014 Kill All Processes",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Text(
            text = "WSS connection, discovery engine, and all background threads will be terminated. Zero battery drain.",
            color = RemoteColors.OnSurfaceDim.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RemoteColors.OnSurfaceDim,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = RemoteColors.OnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                color = RemoteColors.OnSurfaceDim.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        trailing()
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = RemoteColors.NeonCyan,
    checkedTrackColor = RemoteColors.NeonCyan.copy(alpha = 0.3f),
    uncheckedThumbColor = RemoteColors.OnSurfaceDim,
    uncheckedTrackColor = RemoteColors.ButtonMid
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = RemoteColors.NeonCyan,
    activeTrackColor = RemoteColors.NeonCyan,
    inactiveTrackColor = RemoteColors.ButtonMid
)
