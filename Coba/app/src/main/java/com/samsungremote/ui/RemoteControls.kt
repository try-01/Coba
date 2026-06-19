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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Row 1: Power + Source + Exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonPresets.power(onClick = { onKey(SamsungRemoteKey.POWER) }, haptic = hapticEnabled)
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.SOURCE) }, label = "SOURCE", haptic = hapticEnabled)
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.EXIT) }, label = "EXIT", haptic = hapticEnabled)
        }

        Spacer(Modifier.height(20.dp))

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
                    icon = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Left"
                )
                ButtonPresets.dPadCenter(onClick = { onKey(SamsungRemoteKey.ENTER) }, haptic = hapticEnabled)
                ButtonPresets.dPad(
                    onClick = { onKey(SamsungRemoteKey.RIGHT) },
                    haptic = hapticEnabled,
                    icon = Icons.Filled.KeyboardArrowRight,
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

        Spacer(Modifier.height(20.dp))

        // Row 2: Back + Home + Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.BACK) },
                label = "BACK",
                haptic = hapticEnabled,
                icon = Icons.Filled.ArrowBack,
                contentDescription = "Back"
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.HOME) },
                label = "HOME",
                haptic = hapticEnabled,
                icon = Icons.Filled.Home,
                contentDescription = "Home"
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.MENU) },
                label = "MENU",
                haptic = hapticEnabled,
                icon = Icons.Filled.Menu,
                contentDescription = "Menu"
            )
        }

        Spacer(Modifier.height(24.dp))

        // Row 3: Volume / Channel rockers
        RockerGroup(
            label = "VOLUME",
            onUp = { onKey(SamsungRemoteKey.VOLUME_UP) },
            onDown = { onKey(SamsungRemoteKey.VOLUME_DOWN) },
            haptic = hapticEnabled
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.MUTE) },
                label = "MUTE",
                haptic = hapticEnabled,
                size = 64.dp,
                icon = Icons.Filled.VolumeMute,
                contentDescription = "Mute"
            )
        }
        Spacer(Modifier.height(8.dp))
        RockerGroup(
            label = "CHANNEL",
            onUp = { onKey(SamsungRemoteKey.CHANNEL_UP) },
            onDown = { onKey(SamsungRemoteKey.CHANNEL_DOWN) },
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )
        Spacer(Modifier.width(8.dp))
        RemoteButton(
            onClick = onUp,
            icon = Icons.Filled.VolumeUp,
            size = 44.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            tint = RemoteColors.OnSurface,
            hapticEnabled = haptic
        )
        Spacer(Modifier.width(4.dp))
        RemoteButton(
            onClick = onDown,
            icon = Icons.Filled.VolumeDown,
            size = 44.dp,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
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
            .padding(horizontal = 32.dp, vertical = 8.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                row.forEach { digit ->
                    if (digit.isBlank()) {
                        Spacer(Modifier.size(64.dp))
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

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.GUIDE) }, label = "GUIDE", haptic = hapticEnabled)
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.INFO) }, label = "INFO", haptic = hapticEnabled)
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.SMART_HUB) }, label = "HUB", haptic = hapticEnabled)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TRACKPAD",
            color = RemoteColors.OnSurfaceDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
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

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.MOUSE_LEFT_CLICK) }, label = "CLICK", haptic = hapticEnabled)
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEYBOARD) },
                label = "KEYBOARD",
                haptic = hapticEnabled,
                icon = Icons.Filled.Keyboard,
                contentDescription = "Keyboard"
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "KEYBOARD INPUT",
            color = RemoteColors.OnSurfaceDim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
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
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RemoteColors.OnSurface,
                unfocusedTextColor = RemoteColors.OnSurface,
                cursorColor = RemoteColors.NeonCyan,
                focusedBorderColor = RemoteColors.NeonCyan,
                unfocusedBorderColor = RemoteColors.ButtonBorder,
                focusedLabelColor = RemoteColors.NeonCyan,
                unfocusedLabelColor = RemoteColors.OnSurfaceDim
            )
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.BACK) }, label = "BACKSP", haptic = hapticEnabled)
            RemoteButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                },
                label = "SEND",
                size = 72.dp,
                shape = RoundedCornerShape(14.dp),
                backgroundBrush = Brush.linearGradient(
                    listOf(RemoteColors.NeonCyan, RemoteColors.NeonCyan.copy(alpha = 0.7f))
                ),
                tint = RemoteColors.Background,
                hapticEnabled = hapticEnabled
            )
            ButtonPresets.action(onClick = { onKey(SamsungRemoteKey.KEYBOARD_DONE) }, label = "DONE", haptic = hapticEnabled)
        }

        Spacer(Modifier.height(16.dp))
    }
}
