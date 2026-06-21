package com.example.samsungremote.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Default.PowerSettingsNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionHeader(
    isConnected: Boolean,
    onWakeOnLanClick: () -> Unit,
    onServiceToggle: (Boolean) -> Unit
) {
    Row {
        Text(if (isConnected) "Connected" else "Disconnected")
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onWakeOnLanClick) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Wake on LAN")
        }
        Switch(
            checked = false,
            onCheckedChange = onServiceToggle
        )
    }
}