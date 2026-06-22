package com.tvhanan.ui.remote

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import com.tvhanan.ui.components.DpadSection
import com.tvhanan.ui.components.GlassButton
import com.tvhanan.ui.components.MeshGradientBackground
import com.tvhanan.ui.components.ZoneLabel
import com.tvhanan.ui.theme.AccentWarn
import com.tvhanan.ui.theme.BgBase
import com.tvhanan.ui.theme.ColorKeyBlue
import com.tvhanan.ui.theme.ColorKeyGreen
import com.tvhanan.ui.theme.ColorKeyRed
import com.tvhanan.ui.theme.ColorKeyYellow
import com.tvhanan.ui.theme.ConnectedColor
import com.tvhanan.ui.theme.ConnectingColor
import com.tvhanan.ui.theme.DisconnectedColor
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.GlassSurface
import com.tvhanan.ui.theme.MediaAccent
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.PowerGradientEnd
import com.tvhanan.ui.theme.PowerGradientStart
import com.tvhanan.ui.theme.TextDim
import com.tvhanan.ui.theme.TextFaint
import com.tvhanan.ui.theme.TextPrimary

@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onNavigateToSettings: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val showNumpad by viewModel.showNumpad.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val remoteSize by viewModel.remoteSize.collectAsState()
    val meshEnabled by viewModel.meshEnabled.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (meshEnabled) {
            MeshGradientBackground(Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(BgBase))
        }

        val sizeScale = when (remoteSize) {
            "compact" -> 0.86f
            "large" -> 1.14f
            else -> 1f
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy((18f * sizeScale).dp)
        ) {
            stickyHeader {
                StickyHeaderBar(
                    connectionState = connectionState,
                    onSettingsClick = onNavigateToSettings
                )
            }

            if (errorMessage != null) {
                item {
                    ErrorBanner(
                        message = errorMessage!!,
                        onRetry = { viewModel.connect() }
                    )
                }
            }

            item {
                PowerSourceSleepRow(viewModel = viewModel, scale = sizeScale)
            }

            item {
                ZoneLabel("Navigasi", NavAccent)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DpadSection(
                        onUp = { viewModel.sendKey(RemoteKey.DPAD_UP) },
                        onDown = { viewModel.sendKey(RemoteKey.DPAD_DOWN) },
                        onLeft = { viewModel.sendKey(RemoteKey.DPAD_LEFT) },
                        onRight = { viewModel.sendKey(RemoteKey.DPAD_RIGHT) },
                        onOk = { viewModel.sendKey(RemoteKey.ENTER) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                BackHomeExitRow(viewModel = viewModel, scale = sizeScale)
            }

            item {
                ZoneLabel("Volume & Channel", NavAccent2)
                Spacer(Modifier.height(8.dp))
                VolumeChannelPills(viewModel = viewModel, scale = sizeScale)
            }

            item {
                ZoneLabel("Angka", AccentWarn)
                Spacer(Modifier.height(8.dp))
                NumpadGrid(
                    viewModel = viewModel,
                    showNumpad = showNumpad,
                    scale = sizeScale
                )
            }

            item {
                ZoneLabel(
                    "Smart Hub Color Keys",
                    accentColor = null
                )
                Spacer(Modifier.height(8.dp))
                ColorKeysRow(viewModel = viewModel, scale = sizeScale)
            }

            item {
                ZoneLabel("Media", MediaAccent)
                Spacer(Modifier.height(8.dp))
                MediaTransportRow(viewModel = viewModel, scale = sizeScale)
            }

            item {
                ZoneLabel("Menu & Info", TextFaint)
                Spacer(Modifier.height(8.dp))
                MenuInfoGrid(viewModel = viewModel, scale = sizeScale)
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StickyHeaderBar(
    connectionState: ConnectionState,
    onSettingsClick: () -> Unit
) {
    val (statusText, dotColor) = when (connectionState) {
        ConnectionState.CONNECTED -> "Connected" to ConnectedColor
        ConnectionState.CONNECTING -> "Connecting..." to ConnectingColor
        ConnectionState.DISCONNECTED -> "Disconnected" to DisconnectedColor
        ConnectionState.ERROR -> "Error" to DisconnectedColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBase.copy(alpha = 0.78f))
            .clip(RoundedCornerShape(999.dp))
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = statusText,
                color = TextDim,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "SAMSUNG \u00B7 N4300",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        GlassButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(34.dp),
            cornerRadius = 50,
            hapticFeedback = true
        ) {
            Text(
                text = "\u2699",
                color = TextDim,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = DisconnectedColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            cornerRadius = 12,
            gradient = listOf(NavAccent, NavAccent2)
        ) {
            Text(
                "Coba Lagi",
                color = Color(0xFF06201c),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PowerSourceSleepRow(
    viewModel: RemoteViewModel,
    scale: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassButton(
            onClick = { viewModel.sendKey(RemoteKey.POWER) },
            modifier = Modifier
                .weight(1f)
                .height((60f * scale).dp),
            cornerRadius = 18,
            gradient = listOf(PowerGradientStart.copy(alpha = 0.24f), PowerGradientEnd.copy(alpha = 0.16f)),
            borderColor = PowerGradientStart.copy(alpha = 0.38f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u23FB",
                    color = Color(0xFFFFB199),
                    fontSize = 20.sp
                )
                Text(
                    text = "POWER",
                    color = TextDim,
                    fontSize = 9.5.sp
                )
            }
        }

        GlassButton(
            onClick = { viewModel.sendKey(RemoteKey.SOURCE) },
            modifier = Modifier
                .weight(1f)
                .height((60f * scale).dp),
            cornerRadius = 18
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u25A6",
                    color = TextPrimary,
                    fontSize = 20.sp
                )
                Text(
                    text = "SOURCE",
                    color = TextDim,
                    fontSize = 9.5.sp
                )
            }
        }

        GlassButton(
            onClick = { viewModel.sendKey(RemoteKey.SLEEP) },
            modifier = Modifier
                .weight(1f)
                .height((60f * scale).dp),
            cornerRadius = 18
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u263E",
                    color = TextPrimary,
                    fontSize = 20.sp
                )
                Text(
                    text = "SLEEP",
                    color = TextDim,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

@Composable
private fun BackHomeExitRow(
    viewModel: RemoteViewModel,
    scale: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassButton(
            onClick = { viewModel.sendKey(RemoteKey.BACK) },
            modifier = Modifier
                .weight(1f)
                .height((50f * scale).dp),
            cornerRadius = 18
        ) {
            Text(
                text = "\u2190",
                color = TextPrimary,
                fontSize = 18.sp
            )
        }
        GlassButton(
            onClick = { viewModel.sendKey(RemoteKey.HOME) },
            modifier = Modifier
                .weight(1f)
                .height((50f * scale).dp),
            cornerRadius = 18
        ) {
            Text(
                text = "\u2302",
                color = TextPrimary,
                fontSize = 18.sp
            )
        }
        GlassButton(
            onClick = { viewModel.sendKey(RemoteKey.EXIT) },
            modifier = Modifier
                .weight(1f)
                .height((50f * scale).dp),
            cornerRadius = 18
        ) {
            Text(
                text = "\u2715",
                color = TextPrimary,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun VolumeChannelPills(
    viewModel: RemoteViewModel,
    scale: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PillGroup(
            label = "VOL",
            onMinus = { viewModel.sendKey(RemoteKey.VOL_DOWN) },
            onPlus = { viewModel.sendKey(RemoteKey.VOL_UP) },
            onExtra = { viewModel.sendKey(RemoteKey.MUTE) },
            extraIcon = "\uD83D\uDD0A",
            scale = scale
        )
        PillGroup(
            label = "CH",
            onMinus = { viewModel.sendKey(RemoteKey.CH_DOWN) },
            onPlus = { viewModel.sendKey(RemoteKey.CH_UP) },
            onExtra = { viewModel.sendKey(RemoteKey.CH_LIST) },
            extraIcon = "\u2630",
            scale = scale
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassButton(
                onClick = { viewModel.sendKey(RemoteKey.PRE_CH) },
                modifier = Modifier
                    .weight(1f)
                    .height((48f * scale).dp),
                cornerRadius = 18
            ) {
                Text("PRE-CH", color = TextPrimary, fontSize = 11.sp)
            }
            GlassButton(
                onClick = { viewModel.sendKey(RemoteKey.CH_LIST) },
                modifier = Modifier
                    .weight(1f)
                    .height((48f * scale).dp),
                cornerRadius = 18
            ) {
                Text("CH LIST", color = TextPrimary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PillGroup(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onExtra: () -> Unit,
    extraIcon: String,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((54f * scale).dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GlassSurface)
    ) {
        GlassButton(
            onClick = onMinus,
            modifier = Modifier
                .weight(1f)
                .height((54f * scale).dp),
            cornerRadius = 0,
            borderColor = Color.Transparent,
            hapticFeedback = true
        ) {
            Text("\u2212", color = TextPrimary, fontSize = 17.sp)
        }
        GlassButton(
            onClick = {},
            modifier = Modifier
                .weight(1f)
                .height((54f * scale).dp),
            cornerRadius = 0,
            borderColor = Color.Transparent,
            hapticFeedback = false
        ) {
            Text(label, color = TextDim, fontSize = 11.sp)
        }
        GlassButton(
            onClick = onPlus,
            modifier = Modifier
                .weight(1f)
                .height((54f * scale).dp),
            cornerRadius = 0,
            borderColor = Color.Transparent,
            hapticFeedback = true
        ) {
            Text("+", color = TextPrimary, fontSize = 17.sp)
        }
        GlassButton(
            onClick = onExtra,
            modifier = Modifier
                .weight(1f)
                .height((54f * scale).dp),
            cornerRadius = 0,
            borderColor = Color.Transparent,
            hapticFeedback = true
        ) {
            Text(extraIcon, color = TextPrimary, fontSize = 18.sp)
        }
    }
}

@Composable
private fun NumpadGrid(
    viewModel: RemoteViewModel,
    showNumpad: Boolean,
    scale: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            GlassButton(
                onClick = { viewModel.toggleNumpad() },
                modifier = Modifier.size((64f * scale).dp),
                cornerRadius = 18,
                hapticFeedback = true
            ) {
                Text(
                    if (showNumpad) "Tutup" else "Angka",
                    color = TextPrimary,
                    fontSize = 11.sp
                )
            }
        }

        AnimatedVisibility(visible = showNumpad) {
            Column {
                Spacer(Modifier.height(8.dp))
                val numKeys = listOf(
                    listOf(RemoteKey.KEY_1, RemoteKey.KEY_2, RemoteKey.KEY_3),
                    listOf(RemoteKey.KEY_4, RemoteKey.KEY_5, RemoteKey.KEY_6),
                    listOf(RemoteKey.KEY_7, RemoteKey.KEY_8, RemoteKey.KEY_9),
                    listOf(RemoteKey.PRE_CH, RemoteKey.KEY_0, null)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    numKeys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            row.forEach { key ->
                                if (key != null) {
                                    GlassButton(
                                        onClick = { viewModel.sendKey(key) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height((54f * scale).dp),
                                        cornerRadius = 18,
                                        hapticFeedback = true
                                    ) {
                                        Text(
                                            text = key.label,
                                            color = if (key == RemoteKey.PRE_CH) TextDim else TextPrimary,
                                            fontSize = if (key == RemoteKey.PRE_CH) 13.sp else 19.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    GlassButton(
                                        onClick = {},
                                        modifier = Modifier
                                            .weight(1f)
                                            .height((54f * scale).dp),
                                        cornerRadius = 18,
                                        hapticFeedback = false
                                    ) {
                                        Text("", fontSize = 19.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorKeysRow(
    viewModel: RemoteViewModel,
    scale: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ColorKeyButton(
            label = "A",
            color = ColorKeyRed,
            onClick = { viewModel.sendKey(RemoteKey.RED) },
            scale = scale
        )
        ColorKeyButton(
            label = "B",
            color = ColorKeyGreen,
            onClick = { viewModel.sendKey(RemoteKey.GREEN) },
            scale = scale
        )
        ColorKeyButton(
            label = "C",
            color = ColorKeyYellow,
            onClick = { viewModel.sendKey(RemoteKey.YELLOW) },
            scale = scale
        )
        ColorKeyButton(
            label = "D",
            color = ColorKeyBlue,
            onClick = { viewModel.sendKey(RemoteKey.BLUE) },
            scale = scale
        )
    }
}

@Composable
private fun ColorKeyButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    scale: Float
) {
    GlassButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height((46f * scale).dp),
        cornerRadius = 14,
        gradient = listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.08f)),
        borderColor = color.copy(alpha = 0.35f),
        hapticFeedback = true
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MediaTransportRow(
    viewModel: RemoteViewModel,
    scale: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaButton(
            symbol = "\u23EE",
            onClick = { viewModel.sendKey(RemoteKey.REWIND) },
            scale = scale
        )
        MediaButton(
            symbol = "\u25B6",
            onClick = { viewModel.sendKey(RemoteKey.PLAY) },
            scale = scale
        )
        MediaButton(
            symbol = "\u23F8",
            onClick = { viewModel.sendKey(RemoteKey.PAUSE) },
            scale = scale
        )
        MediaButton(
            symbol = "\u23F9",
            onClick = { viewModel.sendKey(RemoteKey.STOP) },
            scale = scale
        )
        MediaButton(
            symbol = "\u23ED",
            onClick = { viewModel.sendKey(RemoteKey.FAST_FORWARD) },
            scale = scale
        )
    }
}

@Composable
private fun MediaButton(
    symbol: String,
    onClick: () -> Unit,
    scale: Float
) {
    GlassButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height((52f * scale).dp),
        cornerRadius = 15,
        gradient = listOf(MediaAccent.copy(alpha = 0.14f), MediaAccent2.copy(alpha = 0.10f)),
        borderColor = MediaAccent.copy(alpha = 0.25f),
        hapticFeedback = true
    ) {
        Text(
            text = symbol,
            color = Color(0xFFD9C2FF),
            fontSize = 18.sp
        )
    }
}

@Composable
private fun MenuInfoGrid(
    viewModel: RemoteViewModel,
    scale: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            MenuButton(
                symbol = "\u2630",
                label = "MENU",
                onClick = { viewModel.sendKey(RemoteKey.MENU) },
                scale = scale
            )
            MenuButton(
                symbol = "\u25A7",
                label = "GUIDE",
                onClick = { viewModel.sendKey(RemoteKey.GUIDE) },
                scale = scale
            )
            MenuButton(
                symbol = "\u24D8",
                label = "INFO",
                onClick = { viewModel.sendKey(RemoteKey.INFO) },
                scale = scale
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            MenuButton(
                symbol = "\u2699",
                label = "SETTINGS",
                onClick = { viewModel.sendKey(RemoteKey.PICTURE_SIZE) },
                scale = scale
            )
            MenuButton(
                symbol = "\uD83D\uDD14",
                label = "P.SIZE",
                onClick = { viewModel.sendKey(RemoteKey.PICTURE_SIZE) },
                scale = scale
            )
            MenuButton(
                symbol = "CC",
                label = "CC/VD",
                onClick = { viewModel.sendKey(RemoteKey.CAPTION) },
                scale = scale
            )
        }
    }
}

@Composable
private fun MenuButton(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    scale: Float
) {
    GlassButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height((58f * scale).dp),
        cornerRadius = 18,
        hapticFeedback = true
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = symbol,
                color = TextPrimary.copy(alpha = 0.85f),
                fontSize = 18.sp
            )
            Text(
                text = label,
                color = TextDim,
                fontSize = 10.sp
            )
        }
    }
}
