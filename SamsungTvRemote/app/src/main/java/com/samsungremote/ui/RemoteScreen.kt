package com.samsungremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(RemoteColors.BackgroundDeep)
                holographicGrid()
                vignetteGradients()
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ConnectionHeader(
                connectionState = uiState.connectionState,
                showConnectionSheet = uiState.showConnectionSheet,
                onToggleConnectionSheet = { viewModel.setShowConnectionSheet(!uiState.showConnectionSheet) },
                onWakeOnLan = {
                    val mac = uiState.manualMac
                    if (mac.isNotBlank()) viewModel.wakeOnLan(mac)
                },
                onSettingsClick = { viewModel.setShowSettings(true) }
            )

            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .drawBehind {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.3f),
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawRect(
                            brush = RemoteBrushes.panelBg,
                            size = Size(size.width, size.height)
                        )
                        drawRoundRect(
                            color = RemoteColors.PanelBorder,
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawLine(
                            color = RemoteColors.NeonCyan.copy(alpha = 0.4f),
                            start = Offset(size.width * 0.3f, 0f),
                            end = Offset(size.width * 0.7f, 0f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    .padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (i == pagerState.currentPage) 20.dp else 4.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i == pagerState.currentPage) RemoteColors.NeonCyan
                                    else RemoteColors.OnSurfaceDim.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) { page ->
                    when (page) {
                        0 -> NavigationPage(
                            onKey = viewModel::sendKey,
                            buttonScale = uiState.buttonScale,
                            hapticEnabled = uiState.hapticEnabled
                        )
                        1 -> NumpadPage(
                            onKey = viewModel::sendKey,
                            onSendText = viewModel::sendText,
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

    if (uiState.showConnectionSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowConnectionSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RemoteColors.Panel,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
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

    if (uiState.showSettings) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowSettings(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RemoteColors.Panel,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
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
                onExitApp = viewModel::exitApp
            )
        }
    }
}
