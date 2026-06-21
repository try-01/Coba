package com.example.samsungremote.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.samsungremote.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()

    Column {
        ListItem(
            headlineContent = { Text("Keep Screen On") },
            trailingContent = {
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )
            }
        )
    }
}