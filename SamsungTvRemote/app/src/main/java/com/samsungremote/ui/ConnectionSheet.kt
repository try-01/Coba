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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Title ────────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.DeviceHub,
                    contentDescription = null,
                    tint = RemoteColors.NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "TV Connection",
                    style = MaterialTheme.typography.titleLarge,
                    color = RemoteColors.OnSurface
                )
            }
        }

        // ── Current status ───────────────────────────────────────────
        item {
            val (statusColor, statusText) = when (connectionState) {
                is TvConnectionState.Connected -> RemoteColors.NeonGreen to "Connected"
                is TvConnectionState.Connecting -> RemoteColors.Amber to "Connecting…"
                is TvConnectionState.Discovering -> RemoteColors.Amber to "Scanning…"
                is TvConnectionState.Error -> RemoteColors.ErrorRed to "Error"
                else -> RemoteColors.ErrorRed to "Disconnected"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (connectionState is TvConnectionState.Connected) {
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RemoteColors.ErrorRed.copy(alpha = 0.2f),
                            contentColor = RemoteColors.ErrorRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Disconnect", fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Auto-discovery section ───────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered TVs",
                    style = MaterialTheme.typography.labelLarge,
                    color = RemoteColors.OnSurfaceDim
                )
                RemoteButton(
                    onClick = onRefresh,
                    icon = Icons.Filled.Refresh,
                    size = 36.dp,
                    shape = CircleShape,
                    tint = RemoteColors.NeonCyan,
                    contentDescription = "Refresh"
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
                        modifier = Modifier.size(20.dp),
                        color = RemoteColors.NeonCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Scanning network…",
                        color = RemoteColors.OnSurfaceDim,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (discoveredTvs.isEmpty() && !isDiscovering) {
            item {
                Text(
                    text = "No TVs found. Try refreshing or enter details manually below.",
                    color = RemoteColors.OnSurfaceDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(discoveredTvs) { tv ->
            DiscoveredTvRow(
                tv = tv,
                onClick = { onConnect(tv.ip, tv.mac ?: "", null) }
            )
        }

        // ── Divider ──────────────────────────────────────────────────
        item { HorizontalDivider(color = RemoteColors.ButtonMid) }

        // ── Manual entry ─────────────────────────────────────────────
        item {
            Text(
                text = "Manual Connection",
                style = MaterialTheme.typography.labelLarge,
                color = RemoteColors.OnSurfaceDim
            )
        }

        item {
            OutlinedTextField(
                value = manualIp,
                onValueChange = onManualIpChange,
                label = { Text("IP Address") },
                placeholder = { Text("192.168.1.100") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = manualMac,
                onValueChange = onManualMacChange,
                label = { Text("MAC Address") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = manualToken,
                onValueChange = onManualTokenChange,
                label = { Text("Token (optional)") },
                placeholder = { Text("Leave blank for first-time pairing") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (manualIp.isNotBlank() && manualMac.isNotBlank()) {
                            onConnect(
                                manualIp,
                                manualMac,
                                manualToken.ifBlank { null }
                            )
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = {
                    onConnect(manualIp, manualMac, manualToken.ifBlank { null })
                },
                enabled = manualIp.isNotBlank() && manualMac.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RemoteColors.NeonCyan,
                    contentColor = RemoteColors.BackgroundDeep
                )
            ) {
                Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Connect", fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DiscoveredTvRow(
    tv: SamsungTvDiscovery.DiscoveredTv,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RemoteColors.SurfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Wifi,
            contentDescription = null,
            tint = RemoteColors.NeonCyan,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tv.modelName ?: "Samsung TV",
                color = RemoteColors.OnSurface,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = tv.ip,
                color = RemoteColors.OnSurfaceDim,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (tv.mac != null) {
            Text(
                text = "Paired",
                color = RemoteColors.NeonGreen,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = RemoteColors.OnSurface,
    unfocusedTextColor = RemoteColors.OnSurface,
    cursorColor = RemoteColors.NeonCyan,
    focusedBorderColor = RemoteColors.NeonCyan,
    unfocusedBorderColor = RemoteColors.ButtonMid,
    focusedLabelColor = RemoteColors.NeonCyan,
    unfocusedLabelColor = RemoteColors.OnSurfaceDim,
    focusedPlaceholderColor = RemoteColors.OnSurfaceDim.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = RemoteColors.OnSurfaceDim.copy(alpha = 0.3f)
)
