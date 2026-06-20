package com.samsungremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is TvConnectionState.Connected

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "// Settings",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.em,
            color = RemoteColors.NeonCyan.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(4.dp))

        GlassSection {
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
        }

        GlassSection {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = null,
                        tint = RemoteColors.OnSurfaceMid,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Button Scale",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = RemoteColors.OnSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "0.5×",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = RemoteColors.OnSurfaceDim
                    )
                    Slider(
                        value = buttonScale,
                        onValueChange = onScaleChange,
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = sliderColors()
                    )
                    Text(
                        "2.0×",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = RemoteColors.OnSurfaceDim
                    )
                }
            }
        }

        GlassSection {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PowerOff,
                    contentDescription = null,
                    tint = RemoteColors.OnSurfaceMid,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remote Service",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = RemoteColors.OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (serviceEnabled) "Active — scan & connect enabled"
                        else "Disabled — saves battery",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = RemoteColors.OnSurfaceDim
                    )
                }
                Switch(
                    checked = serviceEnabled,
                    onCheckedChange = { onServiceToggle() },
                    colors = switchColors()
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (isConnected) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RemoteColors.Amber.copy(alpha = 0.12f),
                    contentColor = RemoteColors.Amber
                )
            ) {
                Icon(Icons.Filled.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Disconnect from TV",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.16.em
                )
            }
        }

        Button(
            onClick = onExitApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RemoteColors.PowerRed.copy(alpha = 0.12f),
                contentColor = RemoteColors.PowerRed
            )
        ) {
            Icon(Icons.Filled.Dangerous, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Exit App",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.16.em
            )
        }

        Text(
            text = "Disconnects the TV, shuts down all background threads, and kills the process. Zero battery drain.",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = RemoteColors.OnSurfaceDim.copy(alpha = 0.6f),
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GlassSection(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RemoteColors.Key)
            .drawBehind {
                drawRoundRect(
                    color = RemoteColors.KeyBorder,
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1f)
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        content()
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
            tint = RemoteColors.OnSurfaceMid,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = RemoteColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = RemoteColors.OnSurfaceDim.copy(alpha = 0.7f)
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
    uncheckedTrackColor = RemoteColors.KeyBorder
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = RemoteColors.NeonCyan,
    activeTrackColor = RemoteColors.NeonCyan,
    inactiveTrackColor = RemoteColors.KeyBorder
)
