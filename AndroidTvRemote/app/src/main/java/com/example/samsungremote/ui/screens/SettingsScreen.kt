package com.example.samsungremote.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SettingsScreen(viewModel: com.example.samsungremote.viewmodel.SettingsViewModel) {
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()

    Column {
        androidx.compose.material3.ListItem(
            headlineText = { Text("Keep Screen On") },
            trailingContent = {
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )
            }
        )
    }
}