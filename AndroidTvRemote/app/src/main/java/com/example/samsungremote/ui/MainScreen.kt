package com.example.samsungremote.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.samsungremote.ui.components.ConnectionHeader
import com.example.samsungremote.ui.components.RemoteControlPager
import com.example.samsungremote.utils.ConnectionState
import com.example.samsungremote.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        ConnectionHeader(
            isConnected = connectionState is ConnectionState.Connected,
            onWakeOnLanClick = { },
            onServiceToggle = { }
        )
        RemoteControlPager(
            onCommand = { command -> viewModel.sendCommand(command) }
        )
    }
}