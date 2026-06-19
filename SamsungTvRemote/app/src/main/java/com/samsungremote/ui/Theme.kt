package com.samsungremote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────

object RemoteColors {
    // Background – deep space gradient
    val BackgroundDeep = Color(0xFF05050E)
    val BackgroundMid = Color(0xFF0B0B1A)
    val BackgroundTop = Color(0xFF12122A)

    val Surface = Color(0xFF141428)
    val SurfaceVariant = Color(0xFF1A1A36)
    val SurfaceElevated = Color(0xFF222244)

    // Neon accents
    val NeonCyan = Color(0xFF00D4FF)
    val NeonCyanGlow = Color(0x4400D4FF)
    val NeonPurple = Color(0xFF7B61FF)
    val NeonPink = Color(0xFFFF6B9D)
    val NeonGreen = Color(0xFF00FF88)
    val Amber = Color(0xFFFFB800)
    val ErrorRed = Color(0xFFFF3355)
    val PowerRed = Color(0xFFDD2244)
    val PowerRedGlow = Color(0x44DD2244)

    // Button surface gradients (metallic/glass)
    val ButtonTop = Color(0xFF2E2E52)
    val ButtonMid = Color(0xFF222244)
    val ButtonBot = Color(0xFF181838)
    val ButtonHighlight = Color(0x22FFFFFF)
    val ButtonShadow = Color(0x44000000)

    // Text
    val OnSurface = Color(0xFFE8E8F0)
    val OnSurfaceDim = Color(0xFF7A7A9A)
}

// ── Brushes ──────────────────────────────────────────────────

object RemoteBrushes {
    val background: Brush = Brush.radialGradient(
        colors = listOf(RemoteColors.BackgroundTop, RemoteColors.BackgroundMid, RemoteColors.BackgroundDeep),
        center = Offset(0.5f, 0.25f),
        radius = 1.4f
    )

    val glass: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.ButtonTop, RemoteColors.ButtonMid, RemoteColors.ButtonBot),
        start = Offset.Zero,
        end = Offset(0f, 1f)
    )

    val glassPressed: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.ButtonMid, RemoteColors.ButtonBot, RemoteColors.ButtonBot),
        start = Offset.Zero,
        end = Offset(0f, 1f)
    )

    val power: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.PowerRed, RemoteColors.PowerRed.copy(alpha = 0.7f), RemoteColors.ErrorRed),
        start = Offset.Zero,
        end = Offset(0f, 1f)
    )

    val powerPressed: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.ErrorRed, RemoteColors.PowerRed.copy(alpha = 0.6f)),
        start = Offset.Zero,
        end = Offset(0f, 1f)
    )

    val accent: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.NeonCyan, RemoteColors.NeonCyan.copy(alpha = 0.7f)),
        start = Offset.Zero,
        end = Offset(0f, 1f)
    )

    val accentPressed: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.NeonCyan.copy(alpha = 0.8f), RemoteColors.NeonCyan.copy(alpha = 0.5f)),
        start = Offset.Zero,
        end = Offset(0f, 1f)
    )
}

private val DarkScheme = darkColorScheme(
    primary = RemoteColors.NeonCyan,
    secondary = RemoteColors.NeonPurple,
    tertiary = RemoteColors.NeonPink,
    background = RemoteColors.BackgroundDeep,
    surface = RemoteColors.Surface,
    surfaceVariant = RemoteColors.SurfaceVariant,
    error = RemoteColors.ErrorRed,
    onPrimary = RemoteColors.BackgroundDeep,
    onSecondary = RemoteColors.BackgroundDeep,
    onTertiary = RemoteColors.BackgroundDeep,
    onBackground = RemoteColors.OnSurface,
    onSurface = RemoteColors.OnSurface,
    onSurfaceVariant = RemoteColors.OnSurfaceDim,
    outline = RemoteColors.ButtonMid,
    outlineVariant = RemoteColors.SurfaceElevated
)

@Composable
fun SamsungRemoteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
