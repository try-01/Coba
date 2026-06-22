package com.tvhanan.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.ui.components.GlassButton
import com.tvhanan.ui.components.MeshGradientBackground
import com.tvhanan.ui.theme.BgBase
import com.tvhanan.ui.theme.ConnectedColor
import com.tvhanan.ui.theme.DisconnectedColor
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.GlassBorderStrong
import com.tvhanan.ui.theme.GlassSurface
import com.tvhanan.ui.theme.GlassSurfacePressed
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.TextDim
import com.tvhanan.ui.theme.TextFaint
import com.tvhanan.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    tvDevice: TvDevice?,
    onBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToManual: () -> Unit,
    onForgetTv: () -> Unit,
    onExit: () -> Unit,
    onShowFeedback: () -> Unit
) {
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val screenOnEnabled by viewModel.screenOnEnabled.collectAsState()
    val meshEnabled by viewModel.meshBackground.collectAsState()
    val remoteSize by viewModel.remoteSize.collectAsState()
    val event by viewModel.events.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(event) {
        when (event) {
            SettingsEvent.NavigateToScan -> onNavigateToScan()
            SettingsEvent.NavigateToManual -> onNavigateToManual()
            SettingsEvent.NavigateToScanOnForget -> {
                onForgetTv()
                onNavigateToScan()
            }
            SettingsEvent.ExitApp -> onExit()
            SettingsEvent.ShowFeedback -> onShowFeedback()
            null -> {}
        }
        viewModel.onEventHandled()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MeshGradientBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsHeader(onBack = onBack)

            TvInfoCard(device = tvDevice)

            SettingsGroup(
                label = "Koneksi"
            ) {
                SettingsRow(
                    icon = IconPaths.wifi,
                    title = "Hubungkan ulang TV",
                    desc = "Cari ulang & sambungkan ke TV ini",
                    onClick = { onNavigateToScan() }
                )
                SettingsRow(
                    icon = IconPaths.search,
                    title = "Pindai TV lain",
                    desc = "Tambahkan TV baru di jaringan",
                    onClick = onNavigateToScan
                )
                SettingsRow(
                    icon = IconPaths.edit,
                    title = "Sambungkan manual",
                    desc = "Masukkan IP TV secara langsung",
                    onClick = onNavigateToManual
                )
                SettingsRow(
                    icon = IconPaths.refresh,
                    title = "Lupakan TV ini",
                    desc = "Hapus token & data koneksi tersimpan",
                    danger = true,
                    onClick = { viewModel.forgetTv() }
                )
            }

            SettingsGroup(
                label = "Tampilan & Pengalaman"
            ) {
                SettingsToggleRow(
                    icon = IconPaths.bell,
                    title = "Getar saat tombol ditekan",
                    desc = "Haptic feedback tiap tap",
                    checked = hapticEnabled,
                    onCheckedChange = { viewModel.toggleHaptic(it) }
                )
                SettingsToggleRow(
                    icon = IconPaths.sun,
                    title = "Tetap terang di tangan",
                    desc = "Cegah layar HP redup saat dipakai",
                    checked = screenOnEnabled,
                    onCheckedChange = { viewModel.toggleScreenOn(it) }
                )
                SettingsToggleRow(
                    icon = IconPaths.moon,
                    title = "Latar belakang dinamis",
                    desc = "Efek aurora di background app",
                    checked = meshEnabled,
                    onCheckedChange = { viewModel.toggleMeshBackground(it) }
                )
            }

            SettingsGroup(
                label = "Ukuran Tampilan Remote"
            ) {
                RemoteSizeSelector(
                    selected = remoteSize,
                    onSelect = { viewModel.setRemoteSize(it) }
                )
            }

            SettingsGroup(
                label = "Lainnya"
            ) {
                SettingsRow(
                    icon = IconPaths.info,
                    title = "Tentang aplikasi",
                    desc = "Versi, lisensi, dan info build",
                    trailing = { Text("v1.0.0", color = TextDim, fontSize = 12.sp) },
                    onClick = {}
                )
                SettingsRow(
                    icon = IconPaths.chat,
                    title = "Kirim masukan",
                    desc = "Laporkan bug atau saran fitur",
                    onClick = { viewModel.feedback() }
                )
                SettingsRow(
                    icon = IconPaths.logout,
                    title = "Keluar dari aplikasi",
                    desc = "Tutup remote sepenuhnya",
                    danger = true,
                    onClick = { viewModel.exitApp() }
                )
            }

            Text(
                text = "TV Remote · versi 1.0.0 (build 26062201)",
                color = TextFaint,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 10.dp)
    ) {
        GlassButton(
            onClick = onBack,
            modifier = Modifier.size(38.dp),
            cornerRadius = 50,
            hapticFeedback = true
        ) {
            Text(
                text = "\u2190",
                color = TextPrimary,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Pengaturan",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TvInfoCard(device: TvDevice?) {
    if (device == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NavAccent.copy(alpha = 0.10f),
                        NavAccent2.copy(alpha = 0.07f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .border(1.dp, NavAccent.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(GlassSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2302",
                    color = NavAccent,
                    fontSize = 24.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = device.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "N-Series · Tizen 5.0",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(ConnectedColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ConnectedColor, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(6.dp))
            Text("Terhubung", color = ConnectedColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetaItem("Alamat IP", device.ipAddress)
            MetaItem("Port", "8001 (WS)")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetaItem("Alamat MAC", device.macAddress ?: "-")
            MetaItem("Sinyal", "Bagus")
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = TextFaint,
            fontSize = 10.sp,
            letterSpacing = 0.06.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable Column.() -> Unit
) {
    Column {
        Text(
            text = label,
            color = TextFaint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.10.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurface, RoundedCornerShape(18.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(
    icon: String,
    title: String,
    desc: String,
    danger: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(GlassSurface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = if (danger) DisconnectedColor else TextDim,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (danger) DisconnectedColor else TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = desc,
                color = TextFaint,
                fontSize = 11.5.sp
            )
        }
        if (trailing != null) {
            trailing()
        } else if (!danger) {
            Text(
                text = "\u203A",
                color = TextFaint,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: String,
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(GlassSurface, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = TextDim,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = desc,
                color = TextFaint,
                fontSize = 11.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NavAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun RemoteSizeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "compact" to "Kompak",
        "fit" to "Pas di layar",
        "large" to "Besar"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isActive = selected == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isActive)
                            Brush.linearGradient(listOf(NavAccent, NavAccent2))
                        else
                            GlassSurface
                    )
                    .border(
                        1.dp,
                        if (isActive) NavAccent.copy(alpha = 0.4f) else GlassBorder,
                        RoundedCornerShape(999.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(value) }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isActive) Color(0xFF06201c) else TextDim,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

object IconPaths {
    val wifi = "\u25C8"
    val search = "\u2315"
    val edit = "\u270E"
    val refresh = "\u21BB"
    val bell = "\u2661"
    val sun = "\u2600"
    val moon = "\u263E"
    val info = "\u24D8"
    val chat = "\u2709"
    val logout = "\u21AA"
}
