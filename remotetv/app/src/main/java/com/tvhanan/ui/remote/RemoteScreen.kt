package com.tvhanan.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvhanan.domain.model.ConnectionState
import com.tvhanan.domain.model.RemoteKey
import com.tvhanan.ui.components.DpadRing
import com.tvhanan.ui.components.HapticGlassButton
import com.tvhanan.ui.components.HapticGlassLabelButton
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
import com.tvhanan.ui.theme.MediaAccent
import com.tvhanan.ui.theme.MediaAccent2
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.NetflixRed
import com.tvhanan.ui.theme.PowerGradientEnd
import com.tvhanan.ui.theme.PowerGradientStart
import com.tvhanan.ui.theme.PrimeBlue
import com.tvhanan.ui.theme.TextDim
import com.tvhanan.ui.theme.TextPrimary
import com.tvhanan.ui.theme.YoutubeRed

/**
 * Layar remote utama. Dibagi 9 zona sesuai preview yang disepakati,
 * ditampilkan via LazyColumn + stickyHeader bawaan Compose Foundation
 * (bukan implementasi manual) supaya status bar + tombol Settings
 * selalu terlihat saat scroll panjang ke bawah.
 *
 * @param scaleFactor faktor skala ukuran tombol, berasal dari
 *   SettingsViewModel.uiPreferences.remoteSize.scaleFactor (1.0 = normal).
 *   Dikalikan ke dp tinggi/ukuran komponen kunci (bukan via Modifier.scale,
 *   supaya touch target & layout flow tetap akurat — beda dari pendekatan
 *   CSS transform:scale() di preview HTML yang sempat menyebabkan overlap).
 */
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onOpenSettings: () -> Unit,
    scaleFactor: Float = 1f
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    // RemoteViewModel di project ini dibuat via remember(ip, port) { ... } di
    // NavGraph, BUKAN lewat ViewModelProvider/viewModel() Compose Navigation.
    // Itu berarti onCleared() (yang berisi disconnect()) tidak otomatis
    // dipanggil oleh lifecycle ViewModel saat navigasi keluar dari screen ini.
    // DisposableEffect memastikan disconnect() tetap terpanggil saat
    // composable ini di-dispose (mis. user menekan back atau navigasi lain),
    // supaya koneksi WebSocket tidak menggantung.
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshGradientBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy((18 * scaleFactor).dp)
        ) {
            stickyHeader {
                RemoteHeaderBar(connectionState = connectionState, onSettingsClick = onOpenSettings)
            }

            errorMessage?.let { message ->
                item { ErrorBanner(message = message, onRetry = { viewModel.connect() }) }
            }

            item { PowerSourceSleepRow(viewModel, scaleFactor) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZoneLabel("Navigasi", accentColor = NavAccent)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        DpadRing(
                            onUp = { viewModel.sendKey(RemoteKey.DPAD_UP) },
                            onDown = { viewModel.sendKey(RemoteKey.DPAD_DOWN) },
                            onLeft = { viewModel.sendKey(RemoteKey.DPAD_LEFT) },
                            onRight = { viewModel.sendKey(RemoteKey.DPAD_RIGHT) },
                            onOk = { viewModel.sendKey(RemoteKey.ENTER) },
                            size = (216 * scaleFactor).dp
                        )
                    }
                    BackHomeExitRow(viewModel, scaleFactor)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZoneLabel("Volume & Channel", accentColor = NavAccent2)
                    VolumeChannelSection(viewModel, scaleFactor)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Angka", accentColor = AccentWarn)
                    NumpadGrid(viewModel, scaleFactor)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel(
                        "Smart Hub Color Keys",
                        accentBrush = Brush.horizontalGradient(listOf(ColorKeyRed, ColorKeyGreen))
                    )
                    ColorKeysRow(viewModel, scaleFactor)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Media", accentColor = MediaAccent)
                    MediaTransportRow(viewModel, scaleFactor)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("Menu & Info", accentColor = TextDim)
                    MenuInfoGrid(viewModel, scaleFactor)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel(
                        "App Pintasan",
                        accentBrush = Brush.horizontalGradient(listOf(NetflixRed, PrimeBlue))
                    )
                    AppShortcutsRow(viewModel, scaleFactor)
                }
            }
        }
    }
}

@Composable
private fun RemoteHeaderBar(connectionState: ConnectionState, onSettingsClick: () -> Unit) {
    val (label, color) = when (connectionState) {
        ConnectionState.CONNECTED -> "Connected" to ConnectedColor
        ConnectionState.CONNECTING -> "Menghubungkan..." to ConnectingColor
        ConnectionState.DISCONNECTED -> "Terputus" to DisconnectedColor
        ConnectionState.ERROR -> "Error" to DisconnectedColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgBase.copy(alpha = 0.78f), RoundedCornerShape(999.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(999.dp))
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.size(width = 8.dp, height = 1.dp))
        Text(text = label, color = TextDim, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "SAMSUNG · N4300",
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.size(width = 10.dp, height = 1.dp))

        HapticGlassButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(34.dp),
            shape = CircleShape
        ) {
            Text("\u2699", color = TextDim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DisconnectedColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .border(1.dp, DisconnectedColor.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(text = message, color = DisconnectedColor, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(10.dp))
        HapticGlassButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Coba Lagi", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PowerSourceSleepRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val height = (60 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HapticGlassButton(
            onClick = { viewModel.sendKey(RemoteKey.POWER) },
            modifier = Modifier.weight(1f).height(height),
            gradientColors = listOf(PowerGradientStart.copy(alpha = 0.24f), PowerGradientEnd.copy(alpha = 0.16f)),
            borderColor = PowerGradientStart.copy(alpha = 0.38f)
        ) {
            Text("Power", color = Color(0xFFFFB199), style = MaterialTheme.typography.bodySmall)
        }
        HapticGlassButton(
            onClick = { viewModel.sendKey(RemoteKey.SOURCE) },
            modifier = Modifier.weight(1f).height(height)
        ) {
            Text("Source", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
        }
        HapticGlassButton(
            onClick = { viewModel.sendKey(RemoteKey.HDMI) },
            modifier = Modifier.weight(1f).height(height)
        ) {
            Text("HDMI", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BackHomeExitRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val height = (50 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HapticGlassLabelButton(
            label = "Back",
            onClick = { viewModel.sendKey(RemoteKey.BACK) },
            modifier = Modifier.weight(1f).height(height),
            fontSize = 13.sp
        )
        HapticGlassLabelButton(
            label = "Home",
            onClick = { viewModel.sendKey(RemoteKey.HOME) },
            modifier = Modifier.weight(1f).height(height),
            fontSize = 13.sp
        )
        HapticGlassLabelButton(
            label = "Exit",
            onClick = { viewModel.sendKey(RemoteKey.EXIT) },
            modifier = Modifier.weight(1f).height(height),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun VolumeChannelSection(viewModel: RemoteViewModel, scaleFactor: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PillRow(
            scaleFactor = scaleFactor,
            cells = listOf(
                PillCell("\u2212", isSymbol = true) { viewModel.sendKey(RemoteKey.VOL_DOWN) },
                PillCell("VOL", isSymbol = false) { },
                PillCell("+", isSymbol = true) { viewModel.sendKey(RemoteKey.VOL_UP) },
                PillCell("Mute", isSymbol = false) { viewModel.sendKey(RemoteKey.MUTE) }
            )
        )
        PillRow(
            scaleFactor = scaleFactor,
            cells = listOf(
                PillCell("\u2212", isSymbol = true) { viewModel.sendKey(RemoteKey.CH_DOWN) },
                PillCell("CH", isSymbol = false) { },
                PillCell("+", isSymbol = true) { viewModel.sendKey(RemoteKey.CH_UP) },
                PillCell("List", isSymbol = false) { viewModel.sendKey(RemoteKey.CH_LIST) }
            )
        )
        HapticGlassLabelButton(
            label = "PRE-CH",
            onClick = { viewModel.sendKey(RemoteKey.PRE_CH) },
            modifier = Modifier.fillMaxWidth().height((46 * scaleFactor).dp),
            fontSize = 11.sp
        )
    }
}

private data class PillCell(val label: String, val isSymbol: Boolean, val onClick: () -> Unit)

@Composable
private fun PillRow(cells: List<PillCell>, scaleFactor: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((54 * scaleFactor).dp)
            .background(Color.Transparent, RoundedCornerShape(18.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        cells.forEachIndexed { index, cell ->
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                cells.lastIndex -> RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                else -> RoundedCornerShape(0.dp)
            }
            HapticGlassButton(
                onClick = cell.onClick,
                modifier = Modifier.weight(1f).fillMaxSize(),
                shape = shape,
                borderColor = Color.Transparent
            ) {
                Text(
                    text = cell.label,
                    color = if (cell.isSymbol) TextPrimary else TextDim,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun NumpadGrid(viewModel: RemoteViewModel, scaleFactor: Float) {
    val rows = listOf(
        listOf(RemoteKey.KEY_1, RemoteKey.KEY_2, RemoteKey.KEY_3),
        listOf(RemoteKey.KEY_4, RemoteKey.KEY_5, RemoteKey.KEY_6),
        listOf(RemoteKey.KEY_7, RemoteKey.KEY_8, RemoteKey.KEY_9),
        listOf(RemoteKey.KEY_0)
    )
    val keyHeight = (54 * scaleFactor).dp
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    HapticGlassLabelButton(
                        label = key.label,
                        onClick = { viewModel.sendKey(key) },
                        modifier = Modifier.weight(1f).height(keyHeight),
                        fontSize = 19.sp
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorKeysRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val height = (46 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ColorKeyButton("A", ColorKeyRed, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.RED) }
        ColorKeyButton("B", ColorKeyGreen, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.GREEN) }
        ColorKeyButton("C", ColorKeyYellow, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.YELLOW) }
        ColorKeyButton("D", ColorKeyBlue, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.BLUE) }
    }
}

@Composable
private fun ColorKeyButton(label: String, color: Color, modifier: Modifier, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    HapticGlassButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        gradientColors = listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.07f)),
        borderColor = color.copy(alpha = 0.35f),
        contentColor = color
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun MediaTransportRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    val buttons = listOf(
        "\u23EA" to { viewModel.sendKey(RemoteKey.REWIND) },
        "\u25B6" to { viewModel.sendKey(RemoteKey.PLAY) },
        "\u23F8" to { viewModel.sendKey(RemoteKey.PAUSE) },
        "\u23F9" to { viewModel.sendKey(RemoteKey.STOP) },
        "\u23E9" to { viewModel.sendKey(RemoteKey.FAST_FORWARD) }
    )
    val height = (52 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { (icon, action) ->
            HapticGlassButton(
                onClick = action,
                modifier = Modifier.weight(1f).height(height),
                shape = RoundedCornerShape(15.dp),
                gradientColors = listOf(MediaAccent.copy(alpha = 0.14f), MediaAccent2.copy(alpha = 0.10f)),
                borderColor = MediaAccent.copy(alpha = 0.25f)
            ) {
                Text(icon, color = MediaAccent, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun MenuInfoGrid(viewModel: RemoteViewModel, scaleFactor: Float) {
    val items = listOf(
        Triple("Menu", RemoteKey.MENU, "\u2630"),
        Triple("Guide", RemoteKey.GUIDE, "\uD83D\uDCC5"),
        Triple("Info", RemoteKey.INFO, "\u2139")
    )
    val height = (58 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items.forEach { (label, key, icon) ->
            HapticGlassButton(
                onClick = { viewModel.sendKey(key) },
                modifier = Modifier.weight(1f).height(height),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(icon, color = TextPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(label, color = TextDim, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AppShortcutsRow(viewModel: RemoteViewModel, scaleFactor: Float) {
    // Catatan: belum ada RemoteKey khusus utk deep-link app Smart Hub di domain
    // model saat ini, jadi dipetakan ke SOURCE sbg placeholder. Untuk dukungan
    // penuh, tambahkan RemoteKey baru (mis. APP_NETFLIX) lalu petakan di
    // SamsungKeyMapper begitu protokol deep-link Smart Hub diimplementasikan.
    val height = (54 * scaleFactor).dp
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppShortcutButton("NETFLIX", NetflixRed, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.SOURCE) }
        AppShortcutButton("PRIME", PrimeBlue, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.SOURCE) }
        AppShortcutButton("YOUTUBE", YoutubeRed, Modifier.weight(1f), height) { viewModel.sendKey(RemoteKey.SOURCE) }
    }
}

@Composable
private fun AppShortcutButton(label: String, color: Color, modifier: Modifier, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    HapticGlassButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(16.dp),
        gradientColors = listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.06f)),
        borderColor = color.copy(alpha = 0.32f),
        contentColor = color
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
