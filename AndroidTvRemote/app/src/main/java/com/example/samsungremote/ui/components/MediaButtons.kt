package com.example.samsungremote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons.Default.Home
import androidx.compose.material.icons.Icons.Default.PlayArrow
import androidx.compose.material.icons.Icons.Default.Pause
import androidx.compose.material.icons.Icons.Default.Stop
import androidx.compose.material.icons.Icons.Default.VolumeUp
import androidx.compose.material.icons.Icons.Default.VolumeDown
import androidx.compose.runtime.Composable

@Composable
fun MediaButtons(onCommand: (String) -> Unit) {
    Column {
        IconButton(onClick = { onCommand("HOME") }) {
            Icon(Icons.Default.Home, contentDescription = "Home")
        }
        IconButton(onClick = { onCommand("PLAY") }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
        IconButton(onClick = { onCommand("PAUSE") }) {
            Icon(Icons.Default.Pause, contentDescription = "Pause")
        }
        IconButton(onClick = { onCommand("STOP") }) {
            Icon(Icons.Default.Stop, contentDescription = "Stop")
        }
        IconButton(onClick = { onCommand("VOL_UP") }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up")
        }
        IconButton(onClick = { onCommand("VOL_DOWN") }) {
            Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down")
        }
    }
}