package com.samsungremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Cast
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.samsungremote.SamsungRemoteKey

// ── Page 1: Navigation / Control ─────────────────────────────

@Composable
fun NavigationPage(
    onKey: (SamsungRemoteKey) -> Unit,
    buttonScale: Float = 1f,
    hapticEnabled: Boolean = true
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Power / Source / Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ButtonPresets.power(
                onClick = { onKey(SamsungRemoteKey.KEY_POWER) },
                modifier = Modifier.weight(1f),
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_SOURCE) },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Cast,
                label = "Source",
                contentDescription = "Source",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_MENU) },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Menu,
                label = "Menu",
                contentDescription = "Menu",
                haptic = hapticEnabled
            )
        }

        // Rockers: Volume + Channel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RockerGroup(
                label = "VOL",
                onUp = { onKey(SamsungRemoteKey.KEY_VOLUP) },
                onDown = { onKey(SamsungRemoteKey.KEY_VOLDOWN) },
                upIcon = Icons.AutoMirrored.Filled.VolumeUp,
                downIcon = Icons.AutoMirrored.Filled.VolumeDown,
                haptic = hapticEnabled,
                modifier = Modifier.weight(1f)
            )
            RockerGroup(
                label = "CH",
                onUp = { onKey(SamsungRemoteKey.KEY_CHUP) },
                onDown = { onKey(SamsungRemoteKey.KEY_CHDOWN) },
                upIcon = Icons.Filled.KeyboardArrowUp,
                downIcon = Icons.Filled.KeyboardArrowDown,
                haptic = hapticEnabled,
                modifier = Modifier.weight(1f)
            )
        }

        // D-Pad
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DPad(
                onUp = { onKey(SamsungRemoteKey.KEY_UP) },
                onDown = { onKey(SamsungRemoteKey.KEY_DOWN) },
                onLeft = { onKey(SamsungRemoteKey.KEY_LEFT) },
                onRight = { onKey(SamsungRemoteKey.KEY_RIGHT) },
                onCenter = { onKey(SamsungRemoteKey.KEY_ENTER) },
                haptic = hapticEnabled
            )
        }

        // Func row: Back / Home / Mute / Exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_RETURN) },
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Back",
                contentDescription = "Back",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_HOME) },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Home,
                label = "Home",
                contentDescription = "Home",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_MUTE) },
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                label = "Mute",
                contentDescription = "Mute",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_EXIT) },
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Exit",
                contentDescription = "Exit",
                haptic = hapticEnabled
            )
        }

        // Media row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ButtonPresets.media(
                onClick = { onKey(SamsungRemoteKey.KEY_REWIND) },
                icon = Icons.Filled.FastRewind,
                haptic = hapticEnabled
            )
            ButtonPresets.media(
                onClick = { onKey(SamsungRemoteKey.KEY_PLAY) },
                icon = Icons.Filled.PlayArrow,
                haptic = hapticEnabled
            )
            ButtonPresets.media(
                onClick = { onKey(SamsungRemoteKey.KEY_PAUSE) },
                icon = Icons.Filled.Pause,
                haptic = hapticEnabled
            )
            ButtonPresets.media(
                onClick = { onKey(SamsungRemoteKey.KEY_FF) },
                icon = Icons.Filled.FastForward,
                haptic = hapticEnabled
            )
            ButtonPresets.media(
                onClick = { onKey(SamsungRemoteKey.KEY_STOP) },
                icon = Icons.Filled.Stop,
                haptic = hapticEnabled
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Page 2: Numpad + Text ────────────────────────────────────

@Composable
fun NumpadPage(
    onKey: (SamsungRemoteKey) -> Unit,
    onSendText: (String) -> Unit = {},
    buttonScale: Float = 1f,
    hapticEnabled: Boolean = true
) {
    var textInput by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel("Number Pad")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadButton("1", onClick = { onKey(SamsungRemoteKey.KEY_1) }, haptic = hapticEnabled)
            NumpadButton("2", onClick = { onKey(SamsungRemoteKey.KEY_2) }, haptic = hapticEnabled)
            NumpadButton("3", onClick = { onKey(SamsungRemoteKey.KEY_3) }, haptic = hapticEnabled)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadButton("4", onClick = { onKey(SamsungRemoteKey.KEY_4) }, haptic = hapticEnabled)
            NumpadButton("5", onClick = { onKey(SamsungRemoteKey.KEY_5) }, haptic = hapticEnabled)
            NumpadButton("6", onClick = { onKey(SamsungRemoteKey.KEY_6) }, haptic = hapticEnabled)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadButton("7", onClick = { onKey(SamsungRemoteKey.KEY_7) }, haptic = hapticEnabled)
            NumpadButton("8", onClick = { onKey(SamsungRemoteKey.KEY_8) }, haptic = hapticEnabled)
            NumpadButton("9", onClick = { onKey(SamsungRemoteKey.KEY_9) }, haptic = hapticEnabled)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumpadButton("↶", onClick = { onKey(SamsungRemoteKey.KEY_PRECH) }, haptic = hapticEnabled)
            NumpadButton("0", onClick = { onKey(SamsungRemoteKey.KEY_0) }, haptic = hapticEnabled)
            NumpadButton("100+", onClick = { onKey(SamsungRemoteKey.KEY_PLUS100) }, haptic = hapticEnabled)
        }

        Spacer(Modifier.height(4.dp))
        SectionLabel("Send Text")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        "Type and send to TV…",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = RemoteColors.OnSurfaceDim
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (textInput.isNotBlank()) onKey(SamsungRemoteKey.KEY_ENTER)
                }),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RemoteColors.OnSurface,
                    fontSize = 13.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RemoteColors.OnSurface,
                    unfocusedTextColor = RemoteColors.OnSurface,
                    cursorColor = RemoteColors.NeonCyan,
                    focusedBorderColor = RemoteColors.KeyBorder,
                    unfocusedBorderColor = RemoteColors.KeyBorder,
                    focusedPlaceholderColor = RemoteColors.OnSurfaceDim,
                    unfocusedPlaceholderColor = RemoteColors.OnSurfaceDim.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            RemoteButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                },
                icon = Icons.Filled.Check,
                size = 48.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundBrush = RemoteBrushes.glass,
                tint = RemoteColors.NeonCyan,
                glowColor = RemoteColors.CyanGlow,
                contentDescription = "Send text",
                hapticEnabled = hapticEnabled
            )
        }

        Text(
            text = "// Press a key above or type text to send",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = RemoteColors.OnSurfaceDim,
            modifier = Modifier.padding(start = 2.dp)
        )

        Spacer(Modifier.height(8.dp))
    }
}

// ── Page 3: Smart / Apps ─────────────────────────────────────

@Composable
fun SmartPage(
    onKey: (SamsungRemoteKey) -> Unit,
    onSendText: (String) -> Unit,
    buttonScale: Float = 1f,
    hapticEnabled: Boolean = true
) {
    var textInput by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel("Smart Keyboard")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_RETURN) },
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Back",
                contentDescription = "Back",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_ENTER) },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.TouchApp,
                label = "Click",
                contentDescription = "Enter",
                haptic = hapticEnabled
            )
            ButtonPresets.action(
                onClick = { onKey(SamsungRemoteKey.KEY_KEYBOARD) },
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Keyboard,
                label = "Keyboard",
                contentDescription = "Keyboard toggle",
                haptic = hapticEnabled
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        "Type text…",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = RemoteColors.OnSurfaceDim
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = RemoteColors.OnSurface,
                    fontSize = 13.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = RemoteColors.OnSurface,
                    unfocusedTextColor = RemoteColors.OnSurface,
                    cursorColor = RemoteColors.NeonCyan,
                    focusedBorderColor = RemoteColors.KeyBorder,
                    unfocusedBorderColor = RemoteColors.KeyBorder,
                    focusedPlaceholderColor = RemoteColors.OnSurfaceDim,
                    unfocusedPlaceholderColor = RemoteColors.OnSurfaceDim.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            RemoteButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                },
                icon = Icons.Filled.Check,
                size = 48.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundBrush = RemoteBrushes.glass,
                tint = RemoteColors.NeonCyan,
                glowColor = RemoteColors.CyanGlow,
                contentDescription = "Send",
                hapticEnabled = hapticEnabled
            )
        }

        Text(
            text = "// Text is sent directly to TV input",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = RemoteColors.OnSurfaceDim,
            modifier = Modifier.padding(start = 2.dp)
        )

        Spacer(Modifier.height(4.dp))
        SectionLabel("Quick Apps")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppTile("Netflix", "N", Color(0xFF7A0C0C))
            AppTile("YouTube", "▶", Color(0xFF6A0C0C))
            AppTile("Prime", "P", Color(0xFF0C2E4A))
            AppTile("Disney+", "D+", Color(0xFF081F3D))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppTile("Spotify", "S", Color(0xFF0A4A28))
            AppTile("Browser", "◎", Color(0xFF163850))
            AppTile("SmartTh.", "⌂", Color(0xFF0E2D5A))
            AppTile("BBC iPlayer", "iP", Color(0xFF2A1010))
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Sub-components ────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.22.em,
            color = RemoteColors.NeonCyan.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(RemoteColors.NeonCyan.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun NumpadButton(
    label: String,
    onClick: () -> Unit,
    haptic: Boolean = true
) {
    ButtonPresets.numpad(
        digit = label,
        onClick = onClick,
        modifier = Modifier.weight(1f),
        haptic = haptic
    )
}

@Composable
private fun RockerGroup(
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    upIcon: androidx.compose.ui.graphics.vector.ImageVector,
    downIcon: androidx.compose.ui.graphics.vector.ImageVector,
    haptic: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(RemoteColors.KeyHi, RemoteColors.Key),
                        start = Offset(0f, 0f),
                        end = Offset(0f, 1f)
                    ),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
                drawRoundRect(
                    color = RemoteColors.KeyBorder,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        RemoteButton(
            onClick = onUp,
            icon = upIcon,
            size = 54.dp,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            backgroundBrush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            tint = RemoteColors.OnSurfaceMid,
            glowColor = RemoteColors.CyanGlow.copy(alpha = 0.3f),
            contentDescription = "$label up",
            hapticEnabled = haptic
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RemoteColors.NeonCyan.copy(alpha = 0.06f))
        )
        RemoteButton(
            onClick = onDown,
            icon = downIcon,
            size = 54.dp,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            backgroundBrush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            tint = RemoteColors.OnSurfaceMid,
            glowColor = RemoteColors.CyanGlow.copy(alpha = 0.3f),
            contentDescription = "$label down",
            hapticEnabled = haptic
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.18.em,
            color = RemoteColors.OnSurfaceDim,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onCenter: () -> Unit,
    haptic: Boolean = true
) {
    val dpadSize = 192.dp
    val btnSize = 48.dp

    Box(
        modifier = Modifier
            .size(dpadSize)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF182A42), Color(0xFF0D1B2E)),
                        center = Offset(0.5f, 0.4f),
                        radius = 0.8f
                    )
                )
                drawCircle(
                    color = RemoteColors.NeonCyan.copy(alpha = 0.1f),
                    style = Stroke(width = 1.dp.toPx())
                )
                // Outer ring glow
                drawCircle(
                    color = Color(0xFF060B14).copy(alpha = 0.8f),
                    radius = size.minDimension * 0.5f + 6.dp.toPx(),
                    style = Stroke(width = 6.dp.toPx())
                )
                drawCircle(
                    color = RemoteColors.NeonCyan.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.5f + 7.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
                // Crosshair lines
                drawLine(
                    color = RemoteColors.NeonCyan.copy(alpha = 0.1f),
                    start = Offset(size.width * 0.33f, size.height * 0.5f),
                    end = Offset(size.width * 0.67f, size.height * 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = RemoteColors.NeonCyan.copy(alpha = 0.1f),
                    start = Offset(size.width * 0.5f, size.height * 0.33f),
                    end = Offset(size.width * 0.5f, size.height * 0.67f),
                    strokeWidth = 1.dp.toPx()
                )
                // Shadow beneath outer ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = size.minDimension * 0.5f + 8.dp.toPx(),
                    style = Stroke(width = 4.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Up
        ButtonPresets.dPad(
            onClick = onUp,
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = "DPad Up",
            haptic = haptic,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        // Down
        ButtonPresets.dPad(
            onClick = onDown,
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = "DPad Down",
            haptic = haptic,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        // Left
        ButtonPresets.dPad(
            onClick = onLeft,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "DPad Left",
            haptic = haptic,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        // Right
        ButtonPresets.dPad(
            onClick = onRight,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "DPad Right",
            haptic = haptic,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        // Center OK
        ButtonPresets.dPadCenter(
            onClick = onCenter,
            haptic = haptic,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun AppTile(
    name: String,
    mono: String,
    bg: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RemoteBrushes.glass)
            .border(1.dp, RemoteColors.KeyBorder, RoundedCornerShape(14.dp))
            .clickable { /* launch app would go here */ },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mono,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            color = RemoteColors.OnSurfaceMid,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
