package com.samsungremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsungremote.RemoteViewModel
import com.samsungremote.TvConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = uiState.selectedTab,
        pageCount = { 3 }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setSelectedTab(pagerState.currentPage)
    }

    // ── Connection sheet ──────────────────────────────────────
    if (uiState.showConnectionSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowConnectionSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RemoteColors.Surface,
            shape = MaterialTheme.shapes.large
        ) {
            ConnectionSheetContent(
                connectionState = uiState.connectionState,
                discoveredTvs = uiState.discoveredTvs,
                isDiscovering = uiState.isDiscovering,
                manualIp = uiState.manualIp,
                manualMac = uiState.manualMac,
                manualToken = uiState.manualToken,
                onManualIpChange = viewModel::setManualIp,
                onManualMacChange = viewModel::setManualMac,
                onManualTokenChange = viewModel::setManualToken,
                onConnect = { ip, mac, token ->
                    viewModel.connectToTv(ip, mac, token)
                    viewModel.setShowConnectionSheet(false)
                },
                onDisconnect = {
                    viewModel.disconnect()
                    viewModel.setShowConnectionSheet(false)
                },
                onRefresh = viewModel::startDiscovery
            )
        }
    }

    // ── Settings sheet ─────────────────────────────────────────
    if (uiState.showSettings) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowSettings(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RemoteColors.Surface,
            shape = MaterialTheme.shapes.large
        ) {
            SettingsSheetContent(
                connectionState = uiState.connectionState,
                hapticEnabled = uiState.hapticEnabled,
                buttonScale = uiState.buttonScale,
                serviceEnabled = uiState.serviceEnabled,
                onHapticToggle = viewModel::setHapticEnabled,
                onScaleChange = viewModel::setButtonScale,
                onServiceToggle = viewModel::toggleService,
                onDisconnect = viewModel::disconnect,
                onShutdownServer = viewModel::shutdownServer,
                onExitApp = viewModel::exitApp
            )
        }
    }

    // ── Main scaffold ─────────────────────────────────────────
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RemoteBrushes.background)
    ) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            ConnectionHeader(
                connectionState = uiState.connectionState,
                showConnectionSheet = uiState.showConnectionSheet,
                onToggleConnectionSheet = {
                    viewModel.setShowConnectionSheet(!uiState.showConnectionSheet)
                },
                onWakeOnLan = {
                    val mac = uiState.manualMac
                    if (mac.isNotBlank()) viewModel.wakeOnLan(mac)
                }
            )
        },
        bottomBar = {
            BottomBar(
                selectedTab = pagerState.currentPage,
                onTabChange = viewModel::setSelectedTab,
                onSettingsClick = { viewModel.setShowSettings(true) },
                connectionState = uiState.connectionState
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (page) {
                0 -> NavigationPage(
                    onKey = viewModel::sendKey,
                    buttonScale = uiState.buttonScale,
                    hapticEnabled = uiState.hapticEnabled
                )
                1 -> NumpadPage(
                    onKey = viewModel::sendKey,
                    buttonScale = uiState.buttonScale,
                    hapticEnabled = uiState.hapticEnabled
                )
                2 -> SmartPage(
                    onKey = viewModel::sendKey,
                    onSendText = viewModel::sendText,
                    buttonScale = uiState.buttonScale,
                    hapticEnabled = uiState.hapticEnabled
                )
            }
        }
    }
    }
}

// ── Bottom navigation bar ─────────────────────────────────────

@Composable
private fun BottomBar(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    connectionState: TvConnectionState
) {
    val connected = connectionState is TvConnectionState.Connected
    val accent = if (connected) RemoteColors.NeonGreen else RemoteColors.NeonCyan

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(RemoteColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TabDot(
                icon = Icons.Filled.Gamepad,
                selected = selectedTab == 0,
                accent = accent,
                onClick = { onTabChange(0) }
            )
            TabDot(
                icon = Icons.Filled.Dialpad,
                selected = selectedTab == 1,
                accent = accent,
                onClick = { onTabChange(1) }
            )
            TabDot(
                icon = Icons.Filled.SmartToy,
                selected = selectedTab == 2,
                accent = accent,
                onClick = { onTabChange(2) }
            )
        }

        RemoteButton(
            onClick = onSettingsClick,
            icon = Icons.Filled.Settings,
            size = 36.dp,
            shape = CircleShape,
            tint = RemoteColors.OnSurfaceDim,
            contentDescription = "Settings",
            hapticEnabled = false
        )
    }
}

@Composable
private fun TabDot(
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.15f) else Color.Transparent,
        label = "tabBg"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) accent else RemoteColors.OnSurfaceDim,
        label = "tabTint"
    )

    RemoteButton(
        onClick = onClick,
        icon = icon,
        size = 40.dp,
        shape = CircleShape,
        tint = tint,
        backgroundBrush = Brush.linearGradient(listOf(bg, bg)),
        contentDescription = if (selected) "Selected tab" else null,
        hapticEnabled = false
    )
}
