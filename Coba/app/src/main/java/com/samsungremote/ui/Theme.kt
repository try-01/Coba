package com.samsungremote.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────

object RemoteColors {
    val Background = Color(0xFF0A0A0F)
    val Surface = Color(0xFF141428)
    val SurfaceVariant = Color(0xFF1E1E3A)
    val SurfaceElevated = Color(0xFF252545)
    val NeonCyan = Color(0xFF00D4FF)
    val NeonPurple = Color(0xFF7B61FF)
    val NeonPink = Color(0xFFFF6B9D)
    val NeonGreen = Color(0xFF00FF88)
    val Amber = Color(0xFFFFB800)
    val ErrorRed = Color(0xFFFF3355)
    val PowerRed = Color(0xFFCC2233)
    val OnSurface = Color(0xFFE8E8F0)
    val OnSurfaceDim = Color(0xFF8888AA)
    val ButtonGradientStart = Color(0xFF1E1E3A)
    val ButtonGradientEnd = Color(0xFF2A2A4A)
    val ButtonBorder = Color(0xFF3A3A5A)
    val GlassHighlight = Color(0x33FFFFFF)
    val GlassShadow = Color(0x33000000)
}

private val DarkScheme = darkColorScheme(
    primary = RemoteColors.NeonCyan,
    secondary = RemoteColors.NeonPurple,
    tertiary = RemoteColors.NeonPink,
    background = RemoteColors.Background,
    surface = RemoteColors.Surface,
    surfaceVariant = RemoteColors.SurfaceVariant,
    error = RemoteColors.ErrorRed,
    onPrimary = RemoteColors.Background,
    onSecondary = RemoteColors.Background,
    onTertiary = RemoteColors.Background,
    onBackground = RemoteColors.OnSurface,
    onSurface = RemoteColors.OnSurface,
    onSurfaceVariant = RemoteColors.OnSurfaceDim,
    outline = RemoteColors.ButtonBorder,
    outlineVariant = RemoteColors.SurfaceElevated
)

// ── Theme composable ─────────────────────────────────────────

@Composable
fun SamsungRemoteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
