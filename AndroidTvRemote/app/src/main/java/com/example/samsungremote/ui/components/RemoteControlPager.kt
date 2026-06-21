package com.example.samsungremote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemoteControlPager(
    onCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        NavigationButtons(onCommand = onCommand)
        MediaButtons(onCommand = onCommand)
        NumberButtons(onCommand = onCommand)
    }
}