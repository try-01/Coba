package com.example.samsungremote.ui.components

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RemoteControlPager(
    onCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        pageCount = 3,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 32.dp)
    ) { page ->
        when (page) {
            0 -> NavigationButtons(onCommand = onCommand)
            1 -> MediaButtons(onCommand = onCommand)
            2 -> NumberButtons(onCommand = onCommand)
        }
    }
}