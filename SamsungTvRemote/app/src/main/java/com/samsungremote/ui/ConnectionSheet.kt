package com.samsungremote.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.samsungremote.SamsungTvDiscovery
import com.samsungremote.TvConnectionState

@Composable
fun ConnectionSheetContent(
    connectionState: TvConnectionState,
    discoveredTvs: List<SamsungTvDiscovery.DiscoveredTv>,
    isDiscovering: Boolean,
    manualIp: String,
    manualMac: String,
    manualToken: String,
    onManualIpChange: (String) -> Unit,
    onManualMacChange: (String) -> Unit,
    onManualTokenChange: (String) -> Unit,
    onConnect: (ip: String, mac: String, token: String?) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "// TV Connection",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.em,
                    color = RemoteColors.NeonCyan.copy(alpha = 0.8f)
                )
            }
        }

        item {
            val (statusColor, statusText) = when (connectionState) {
                is TvConnectionState.Connected -> RemoteColors.OkGreen to "Connected"
                is TvConnectionState.Connecting -> RemoteColors.Amber to "Connecting…"
                is TvConnectionState.Discovering -> RemoteColors.Amber to "Scanning…"
                is TvConnectionState.Error -> RemoteColors.PowerRed to "Error: ${connectionState.throwable.localizedMessage?.take(60) ?: "Unknown"}"
                else -> RemoteColors.OnSurfaceDim to "Disconnected"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
                if (connectionState is TvConnectionState.Connected) {
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RemoteColors.PowerRed.copy(alpha = 0.15f),
                            contentColor = RemoteColors.PowerRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Disconnect", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered TVs",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 0.2.em,
                    color = RemoteColors.OnSurfaceDim
                )
                RemoteButton(
                    onClick = onRefresh,
                    icon = Icons.Filled.Refresh,
                    size = 32.dp,
                    shape = CircleShape,
                    tint = RemoteColors.NeonCyan,
                    contentDescription = "Refresh",
                    hapticEnabled = false
                )
            }
        }

        if (isDiscovering) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = RemoteColors.NeonCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Scanning network…",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = RemoteColors.OnSurfaceDim
                    )
                }
            }
        }

        if (discoveredTvs.isEmpty() && !isDiscovering) {
            item {
                Text(
                    text = "No TVs found. Try refreshing or enter IP manually below.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = RemoteColors.OnSurfaceDim,
                    lineHeight = 18.sp
                )
            }
        }

        items(discoveredTvs) { tv ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RemoteColors.SurfaceVariant)
                    .clickable { onConnect(tv.ip, tv.mac ?: "", null) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = RemoteColors.NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tv.modelName ?: "Samsung TV",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = RemoteColors.OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = tv.ip,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = RemoteColors.OnSurfaceDim
                    )
                }
                if (tv.mac != null) {
                    Text(
                        text = "Paired",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = RemoteColors.OkGreen
                    )
                }
            }
        }

        item { HorizontalDivider(color = RemoteColors.KeyBorder) }

        item {
            Text(
                text = "Manual Connection",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 0.2.em,
                color = RemoteColors.OnSurfaceDim
            )
        }

        item {
            OutlinedTextField(
                value = manualIp,
                onValueChange = onManualIpChange,
                label = { Text("IP Address", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                placeholder = { Text("192.168.1.100", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = RemoteColors.OnSurfaceDim) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(10.dp),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RemoteColors.OnSurface,
                    fontSize = 13.sp
                )
            )
        }

        item {
            OutlinedTextField(
                value = manualMac,
                onValueChange = onManualMacChange,
                label = { Text("MAC Address (WoL)", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                placeholder = { Text("AA:BB:CC:DD:EE:FF", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = RemoteColors.OnSurfaceDim) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(10.dp),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RemoteColors.OnSurface,
                    fontSize = 13.sp
                )
            )
        }

        item {
            OutlinedTextField(
                value = manualToken,
                onValueChange = onManualTokenChange,
                label = { Text("Token (optional)", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                placeholder = { Text("Leave blank for first pairing", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = RemoteColors.OnSurfaceDim) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        if (manualIp.isNotBlank() && manualMac.isNotBlank()) {
                            onConnect(manualIp, manualMac, manualToken.ifBlank { null })
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(10.dp),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RemoteColors.OnSurface,
                    fontSize = 13.sp
                )
            )
        }

        item {
            Button(
                onClick = { onConnect(manualIp, manualMac, manualToken.ifBlank { null }) },
                enabled = manualIp.isNotBlank() && manualMac.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RemoteColors.CyanDim.copy(alpha = 0.6f),
                    contentColor = RemoteColors.NeonCyan
                )
            ) {
                Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Connect", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.16.em)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RemoteColors.OnSurface,
    unfocusedTextColor = RemoteColors.OnSurface,
    cursorColor = RemoteColors.NeonCyan,
    focusedBorderColor = RemoteColors.CyanDim.copy(alpha = 0.5f),
    unfocusedBorderColor = RemoteColors.KeyBorder,
    focusedLabelColor = RemoteColors.NeonCyan,
    unfocusedLabelColor = RemoteColors.OnSurfaceDim,
    focusedPlaceholderColor = RemoteColors.OnSurfaceDim.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = RemoteColors.OnSurfaceDim.copy(alpha = 0.3f)
)
