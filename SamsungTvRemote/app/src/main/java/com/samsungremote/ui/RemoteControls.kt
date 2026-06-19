package com.samsungremote.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsungremote.SamsungRemoteKey

// ─────────────────────────────────────────────────────────────
// Page 0 — Navigation
// ─────────────────────────────────────────────────────────────

@Composable
fun NavigationPage(
    onKey: (SamsungRemoteKey) -> Unit,
    buttonScale: Float,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        // Row 1: Power + Source + Exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonPresets.power(onClick = { onKey(SamsungRemoteKey.POWER) }, haptic = hapticEnabled)
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.SOURCE) },
                icon = Icons.Filled.Tv,
                contentDescription = "Source",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.EXIT) },
                icon = Icons.Filled.ExitToApp,
                contentDescription = "Exit",
                haptic = hapticEnabled
            )
        }

        Spacer(Modifier.height(24.dp))

        // D-Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ButtonPresets.dPad(
                onClick = { onKey(SamsungRemoteKey.UP) },
                haptic = hapticEnabled,
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Up"
            )

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ButtonPresets.dPad(
                    onClick = { onKey(SamsungRemoteKey.LEFT) },
                    haptic = hapticEnabled,
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Left"
                )
                ButtonPresets.dPadCenter(
                    onClick = { onKey(SamsungRemoteKey.ENTER) },
                    haptic = hapticEnabled
                )
                ButtonPresets.dPad(
                    onClick = { onKey(SamsungRemoteKey.RIGHT) },
                    haptic = hapticEnabled,
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Right"
                )
            }

            Spacer(Modifier.height(6.dp))

            ButtonPresets.dPad(
                onClick = { onKey(SamsungRemoteKey.DOWN) },
                haptic = hapticEnabled,
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Down"
            )
        }

        Spacer(Modifier.height(24.dp))

        // Row 2: Back + Home + Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.BACK) },
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.HOME) },
                icon = Icons.Filled.Home,
                contentDescription = "Home",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.MENU) },
                icon = Icons.Filled.Menu,
                contentDescription = "Menu",
                haptic = hapticEnabled
            )
        }

        Spacer(Modifier.height(28.dp))

        // Row 3: Volume / Channel rockers
        RockerGroup(
            label = "VOL",
            onUp = { onKey(SamsungRemoteKey.VOLUME_UP) },
            onDown = { onKey(SamsungRemoteKey.VOLUME_DOWN) },
            upIcon = Icons.AutoMirrored.Filled.VolumeUp,
            downIcon = Icons.AutoMirrored.Filled.VolumeDown,
            haptic = hapticEnabled
        )

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.MUTE) },
                icon = Icons.Filled.VolumeOff,
                contentDescription = "Mute",
                haptic = hapticEnabled,
                size = 56.dp
            )
        }

        Spacer(Modifier.height(6.dp))

        RockerGroup(
            label = "CH",
            onUp = { onKey(SamsungRemoteKey.CHANNEL_UP) },
            onDown = { onKey(SamsungRemoteKey.CHANNEL_DOWN) },
            upIcon = Icons.Filled.Tune,
            downIcon = Icons.Filled.Tune,
            haptic = hapticEnabled
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun RockerGroup(
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    upIcon: androidx.compose.ui.graphics.vector.ImageVector,
    downIcon: androidx.compose.ui.graphics.vector.ImageVector,
    haptic: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = RemoteColors.OnSurfaceDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(48.dp)
        )
        Spacer(Modifier.width(8.dp))
        RemoteButton(
            onClick = onUp,
            icon = upIcon,
            size = 44.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
            tint = RemoteColors.OnSurface,
            hapticEnabled = haptic
        )
        Spacer(Modifier.width(6.dp))
        RemoteButton(
            onClick = onDown,
            icon = downIcon,
            size = 44.dp,
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            tint = RemoteColors.OnSurface,
            hapticEnabled = haptic
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Page 1 — Numpad
// ─────────────────────────────────────────────────────────────

@Composable
fun NumpadPage(
    onKey: (SamsungRemoteKey) -> Unit,
    buttonScale: Float,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
            .padding(horizontal = 36.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val digits = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "")
        )

        val keyMap = mapOf(
            "0" to SamsungRemoteKey.KEY_0,
            "1" to SamsungRemoteKey.KEY_1,
            "2" to SamsungRemoteKey.KEY_2,
            "3" to SamsungRemoteKey.KEY_3,
            "4" to SamsungRemoteKey.KEY_4,
            "5" to SamsungRemoteKey.KEY_5,
            "6" to SamsungRemoteKey.KEY_6,
            "7" to SamsungRemoteKey.KEY_7,
            "8" to SamsungRemoteKey.KEY_8,
            "9" to SamsungRemoteKey.KEY_9,
        )

        digits.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                row.forEach { digit ->
                    if (digit.isBlank()) {
                        Spacer(Modifier.size(66.dp))
                    } else {
                        ButtonPresets.numpad(
                            digit = digit,
                            onClick = { keyMap[digit]?.let(onKey) },
                            haptic = hapticEnabled
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.GUIDE) },
                icon = Icons.Filled.ViewList,
                contentDescription = "Guide",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.INFO) },
                icon = Icons.Filled.Info,
                contentDescription = "Info",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.SMART_HUB) },
                icon = Icons.Filled.Apps,
                contentDescription = "Smart Hub",
                haptic = hapticEnabled
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Page 2 — Trackpad / Keyboard
// ─────────────────────────────────────────────────────────────

@Composable
fun SmartPage(
    onKey: (SamsungRemoteKey) -> Unit,
    onSendText: (String) -> Unit,
    buttonScale: Float,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TRACKPAD",
            color = RemoteColors.OnSurfaceDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            TrackpadSurface(
                onSwipe = { direction ->
                    val key = when (direction) {
                        SwipeDirection.UP -> SamsungRemoteKey.UP
                        SwipeDirection.DOWN -> SamsungRemoteKey.DOWN
                        SwipeDirection.LEFT -> SamsungRemoteKey.LEFT
                        SwipeDirection.RIGHT -> SamsungRemoteKey.RIGHT
                    }
                    onKey(key)
                },
                onClick = { onKey(SamsungRemoteKey.ENTER) }
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.MOUSE_LEFT_CLICK) },
                icon = Icons.Filled.TouchApp,
                contentDescription = "Click",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEYBOARD) },
                icon = Icons.Filled.Keyboard,
                contentDescription = "Keyboard",
                haptic = hapticEnabled
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "KEYBOARD INPUT",
            color = RemoteColors.OnSurfaceDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Enter text") },
            placeholder = { Text("Type to send to TV\u2026") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RemoteColors.OnSurface,
                unfocusedTextColor = RemoteColors.OnSurface,
                cursorColor = RemoteColors.NeonCyan,
                focusedBorderColor = RemoteColors.NeonCyan,
                unfocusedBorderColor = RemoteColors.ButtonMid,
                focusedLabelColor = RemoteColors.NeonCyan,
                unfocusedLabelColor = RemoteColors.OnSurfaceDim,
                focusedContainerColor = RemoteColors.SurfaceVariant,
                unfocusedContainerColor = RemoteColors.Surface
            )
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.BACK) },
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Backspace",
                haptic = hapticEnabled
            )
            RemoteButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                },
                label = "SEND",
                size = 72.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundBrush = RemoteBrushes.accent,
                tint = RemoteColors.BackgroundDeep,
                glowColor = RemoteColors.NeonCyanGlow,
                hapticEnabled = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEYBOARD_DONE) },
                icon = Icons.Filled.Check,
                contentDescription = "Done",
                haptic = hapticEnabled
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
