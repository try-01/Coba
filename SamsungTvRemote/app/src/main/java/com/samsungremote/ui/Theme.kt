package com.samsungremote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

object RemoteColors {
    val BackgroundDeep = Color(0xFF060B14)
    val BackgroundMid = Color(0xFF0A1120)
    val Panel = Color(0xFF0D1526)
    val PanelRaised = Color(0xFF111E32)
    val PanelBorder = Color(0x1463B3FF)
    val Key = Color(0xFF0F1C30)
    val KeyHi = Color(0xFF162540)
    val KeyBorder = Color(0x1E63B3FF)
    val NeonCyan = Color(0xFF00D4FF)
    val CyanDim = Color(0xFF0A4F66)
    val CyanGlow = Color(0x4400D4FF)
    val CyanGlowStrong = Color(0x8800D4FF)
    val Violet = Color(0xFF9B5CFF)
    val VioletDim = Color(0xFF3A1F77)
    val VioletGlow = Color(0x509B5CFF)
    val PowerRed = Color(0xFFFF3D5A)
    val PowerRedGlow = Color(0x5AFF3D5A)
    val OkGreen = Color(0xFF00FF88)
    val OkGreenGlow = Color(0x4D00FF88)
    val Amber = Color(0xFFFFB800)
    val OnSurface = Color(0xFFD8EAF8)
    val OnSurfaceDim = Color(0xFF4A6480)
    val OnSurfaceMid = Color(0xFF7A9AB8)
    val Surface = Color(0xFF0D1526)
    val SurfaceVariant = Color(0xFF111E32)
    val SurfaceElevated = Color(0xFF162540)
}

object RemoteBrushes {
    val background: Brush = Brush.radialGradient(
        colors = listOf(Color(0xFF0E1A2E), RemoteColors.BackgroundMid, RemoteColors.BackgroundDeep),
        center = Offset(0.5f, 0.2f),
        radius = 1.6f
    )

    val panelBg: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.PanelRaised, RemoteColors.Panel, RemoteColors.BackgroundDeep.copy(alpha = 0.95f)),
        start = Offset(0f, 0f),
        end = Offset(0f, 1f)
    )

    val glass: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.KeyHi, RemoteColors.Key),
        start = Offset(0f, 0f),
        end = Offset(0f, 1f)
    )

    val glassPressed: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.Key, RemoteColors.Key.copy(alpha = 0.7f)),
        start = Offset(0f, 0f),
        end = Offset(0f, 1f)
    )

    val power: Brush = Brush.linearGradient(
        colors = listOf(Color(0xFF1F0C10), Color(0xFF150809)),
        start = Offset(0f, 0f),
        end = Offset(0f, 1f)
    )

    val powerPressed: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.PowerRed.copy(alpha = 0.15f), RemoteColors.PowerRed.copy(alpha = 0.05f)),
        start = Offset(0f, 0f),
        end = Offset(0f, 1f)
    )

    val accent: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.NeonCyan.copy(alpha = 0.9f), RemoteColors.NeonCyan.copy(alpha = 0.7f)),
        start = Offset(0f, 0f),
        end = Offset(0f, 1f)
    )

    val accentPressed: Brush = Brush.linearGradient(
        colors = listOf(RemoteColors.NeonCyan.copy(alpha = 0.8f), RemoteColors.NeonCyan.copy(alpha = 0.5f)),
        start = Offset(0f, 0f),
        end = Offset(0f, 1f)
    )
}

fun DrawScope.holographicGrid() {
    val gridSize = 40.dp.toPx()
    val gridColor = Color(0x0800D4FF)
    val step = gridSize
    val w = size.width
    val h = size.height

    var y = 0f
    while (y < h) {
        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        y += step
    }
    var x = 0f
    while (x < w) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        x += step
    }
}

fun DrawScope.vignetteGradients() {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(RemoteColors.Violet.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(0.5f, -0.1f),
            radius = 0.8f
        ),
        radius = size.maxDimension * 0.8f,
        center = Offset(size.width * 0.5f, 0f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(RemoteColors.NeonCyan.copy(alpha = 0.07f), Color.Transparent),
            center = Offset(1.1f, 1.1f),
            radius = 0.6f
        ),
        radius = size.maxDimension * 0.6f,
        center = Offset(size.width, size.height)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF091228).copy(alpha = 0.9f), Color.Transparent),
            center = Offset(0f, 0.9f),
            radius = 0.5f
        ),
        radius = size.maxDimension * 0.5f,
        center = Offset.Zero
    )
}

fun DrawScope.glowBorder(
    glowColor: Color = RemoteColors.NeonCyan.copy(alpha = 0.15f),
    cornerRadius: Float = 16.dp.toPx(),
    strokeWidth: Float = 1f
) {
    val rect = androidx.compose.ui.geometry.Rect(
        Offset.Zero,
        Offset(size.width, size.height)
    )
    drawRoundRect(
        glowColor,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
        style = Stroke(width = strokeWidth)
    )
}

private val DarkScheme = darkColorScheme(
    primary = RemoteColors.NeonCyan,
    secondary = RemoteColors.Violet,
    tertiary = RemoteColors.OkGreen,
    background = RemoteColors.BackgroundDeep,
    surface = RemoteColors.Surface,
    surfaceVariant = RemoteColors.SurfaceVariant,
    error = RemoteColors.PowerRed,
    onPrimary = RemoteColors.BackgroundDeep,
    onSecondary = RemoteColors.BackgroundDeep,
    onTertiary = RemoteColors.BackgroundDeep,
    onBackground = RemoteColors.OnSurface,
    onSurface = RemoteColors.OnSurface,
    onSurfaceVariant = RemoteColors.OnSurfaceDim,
    outline = RemoteColors.KeyBorder,
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
