package com.example.samsungremote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Default
import androidx.compose.runtime.Composable

@Composable
fun NavigationButtons(onCommand: (String) -> Unit) {
    Column {
        IconButton(onClick = { onCommand("UP") }) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
        }
        Row {
            IconButton(onClick = { onCommand("LEFT") }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Left")
            }
            IconButton(onClick = { onCommand("ENTER") }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Enter")
            }
            IconButton(onClick = { onCommand("RIGHT") }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Right")
            }
        }
        IconButton(onClick = { onCommand("DOWN") }) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Down")
        }
    }
}