package com.tvhanan.ui.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.tvhanan.ui.theme.ConnectedGreen
import com.tvhanan.ui.theme.ConnectingYellow
import com.tvhanan.ui.theme.DisconnectedRed
import com.tvhanan.ui.theme.DpadActive
import com.tvhanan.ui.theme.DpadGray
import com.tvhanan.ui.theme.PowerRed
import com.tvhanan.ui.theme.RemoteBlue
import com.tvhanan.ui.theme.RemoteGreen
import com.tvhanan.ui.theme.RemoteRed
import com.tvhanan.ui.theme.RemoteYellow

@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onBack: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val showNumpad by viewModel.showNumpad.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        StatusBar(connectionState = connectionState)

        Spacer(modifier = Modifier.height(16.dp))

        PowerRow(viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        DpadSection(viewModel = viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        NavRow(viewModel = viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        VolumeChannelRow(viewModel = viewModel)

        Spacer(modifier = Modifier.height(8.dp))

        NumpadToggle(viewModel = viewModel)

        AnimatedVisibility(visible = showNumpad) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Numpad(viewModel = viewModel)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ColorButtons(viewModel = viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        MediaButtons(viewModel = viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        MiscRow(viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatusBar(connectionState: ConnectionState) {
    val (text, color) = when (connectionState) {
        ConnectionState.CONNECTED -> "Terhubung" to ConnectedGreen
        ConnectionState.CONNECTING -> "Menghubungkan..." to ConnectingYellow
        ConnectionState.DISCONNECTED -> "Terputus" to DisconnectedRed
        ConnectionState.ERROR -> "Error" to DisconnectedRed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PowerRow(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteButton(
            label = "Power",
            onClick = { viewModel.sendKey(RemoteKey.POWER) },
            backgroundColor = PowerRed,
            modifier = Modifier.size(56.dp)
        )
        RemoteButton(
            label = "Src",
            onClick = { viewModel.sendKey(RemoteKey.SOURCE) },
            modifier = Modifier.size(48.dp)
        )
        RemoteButton(
            label = "HDMI",
            onClick = { viewModel.sendKey(RemoteKey.HDMI) },
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun DpadSection(viewModel: RemoteViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier.width(60.dp))
            DpadButton(
                text = "\u25B2",
                onClick = { viewModel.sendKey(RemoteKey.DPAD_UP) }
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DpadButton(
                text = "\u25C0",
                onClick = { viewModel.sendKey(RemoteKey.DPAD_LEFT) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DpadActive),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            DpadButton(
                text = "\u25B6",
                onClick = { viewModel.sendKey(RemoteKey.DPAD_RIGHT) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            Spacer(modifier = Modifier.width(60.dp))
            DpadButton(
                text = "\u25BC",
                onClick = { viewModel.sendKey(RemoteKey.DPAD_DOWN) }
            )
            Spacer(modifier = Modifier.width(60.dp))
        }
    }
}

@Composable
private fun NavRow(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteButton(
            label = "Back",
            onClick = { viewModel.sendKey(RemoteKey.BACK) },
            modifier = Modifier.size(56.dp)
        )
        RemoteButton(
            label = "Home",
            onClick = { viewModel.sendKey(RemoteKey.HOME) },
            modifier = Modifier.size(56.dp)
        )
        RemoteButton(
            label = "Exit",
            onClick = { viewModel.sendKey(RemoteKey.EXIT) },
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
private fun VolumeChannelRow(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RemoteButton(
                label = "Vol+",
                onClick = { viewModel.sendKey(RemoteKey.VOL_UP) },
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            RemoteButton(
                label = "Vol-",
                onClick = { viewModel.sendKey(RemoteKey.VOL_DOWN) },
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            RemoteButton(
                label = "Mute",
                onClick = { viewModel.sendKey(RemoteKey.MUTE) },
                modifier = Modifier.size(52.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RemoteButton(
                label = "CH+",
                onClick = { viewModel.sendKey(RemoteKey.CH_UP) },
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            RemoteButton(
                label = "CH-",
                onClick = { viewModel.sendKey(RemoteKey.CH_DOWN) },
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            RemoteButton(
                label = "List",
                onClick = { viewModel.sendKey(RemoteKey.CH_LIST) },
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun NumpadToggle(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        RemoteButton(
            label = if (viewModel.showNumpad.collectAsState().value) "Tutup" else "Angka",
            onClick = { viewModel.toggleNumpad() },
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun Numpad(viewModel: RemoteViewModel) {
    val numKeys = listOf(
        listOf(RemoteKey.KEY_1, RemoteKey.KEY_2, RemoteKey.KEY_3),
        listOf(RemoteKey.KEY_4, RemoteKey.KEY_5, RemoteKey.KEY_6),
        listOf(RemoteKey.KEY_7, RemoteKey.KEY_8, RemoteKey.KEY_9),
        listOf(RemoteKey.KEY_0, RemoteKey.PRE_CH)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        numKeys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    RemoteButton(
                        label = key.label,
                        onClick = { viewModel.sendKey(key) },
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorButtons(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteButton(
            label = "A",
            onClick = { viewModel.sendKey(RemoteKey.RED) },
            backgroundColor = RemoteRed,
            modifier = Modifier.size(44.dp)
        )
        RemoteButton(
            label = "B",
            onClick = { viewModel.sendKey(RemoteKey.GREEN) },
            backgroundColor = RemoteGreen,
            modifier = Modifier.size(44.dp)
        )
        RemoteButton(
            label = "C",
            onClick = { viewModel.sendKey(RemoteKey.YELLOW) },
            backgroundColor = RemoteYellow,
            modifier = Modifier.size(44.dp)
        )
        RemoteButton(
            label = "D",
            onClick = { viewModel.sendKey(RemoteKey.BLUE) },
            backgroundColor = RemoteBlue,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
private fun MediaButtons(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteButton(
            label = "Play",
            onClick = { viewModel.sendKey(RemoteKey.PLAY) },
            modifier = Modifier.size(48.dp)
        )
        RemoteButton(
            label = "Pause",
            onClick = { viewModel.sendKey(RemoteKey.PAUSE) },
            modifier = Modifier.size(48.dp)
        )
        RemoteButton(
            label = "Rew",
            onClick = { viewModel.sendKey(RemoteKey.REWIND) },
            modifier = Modifier.size(48.dp)
        )
        RemoteButton(
            label = "FF",
            onClick = { viewModel.sendKey(RemoteKey.FAST_FORWARD) },
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun MiscRow(viewModel: RemoteViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteButton(
            label = "Menu",
            onClick = { viewModel.sendKey(RemoteKey.MENU) },
            modifier = Modifier.size(52.dp)
        )
        RemoteButton(
            label = "Guide",
            onClick = { viewModel.sendKey(RemoteKey.GUIDE) },
            modifier = Modifier.size(52.dp)
        )
        RemoteButton(
            label = "Info",
            onClick = { viewModel.sendKey(RemoteKey.INFO) },
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun DpadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(DpadGray),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RemoteButton(
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier.size(56.dp)
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = backgroundColor
        )
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
