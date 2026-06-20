package com.samsungremote.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    icon: ImageVector? = null,
    painter: Painter? = null,
    contentDescription: String? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    size: Dp = 60.dp,
    fontSize: TextUnit = 10.sp,
    tint: Color = RemoteColors.OnSurface,
    backgroundBrush: Brush = RemoteBrushes.glass,
    disabledAlpha: Float = 0.38f,
    glowColor: Color = Color.Transparent,
    hapticEnabled: Boolean = true,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animScale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.94f
            else -> 1f
        },
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(animScale)
            .shadow(
                elevation = if (isPressed && glowColor != Color.Transparent) 8.dp else 4.dp,
                shape = shape,
                ambientColor = if (isPressed) glowColor else Color.Transparent,
                spotColor = if (isPressed) glowColor else Color.Transparent
            )
            .clip(shape)
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height
                if (isPressed && glowColor != Color.Transparent) {
                    drawRoundRect(
                        color = glowColor,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.3f),
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
            .background(backgroundBrush, shape)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            if (hapticEnabled) performHaptic(context)
                            onClick()
                        }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(0.5f, 0.15f),
                        radius = 0.7f
                    )
                )
        )
        if (isPressed && glowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(size * 0.92f)
                    .clip(shape)
                    .background(glowColor.copy(alpha = 0.08f))
            )
        }
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription ?: label,
                tint = if (enabled) tint else tint.copy(alpha = disabledAlpha),
                modifier = Modifier.size(size * 0.50f)
            )
        } else if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription ?: label,
                tint = if (enabled) tint else tint.copy(alpha = disabledAlpha),
                modifier = Modifier.size(size * 0.50f)
            )
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = if (enabled) tint else tint.copy(alpha = disabledAlpha),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

fun performHaptic(context: Context) {
    try {
        val vibrator = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            }
            else -> {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) { }
}

object ButtonPresets {
    @Composable
    fun power(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        haptic: Boolean = true
    ) = RemoteButton(
        onClick = onClick,
        modifier = modifier,
        icon = Icons.Filled.PowerSettingsNew,
        contentDescription = "Power",
        size = 54.dp,
        shape = RoundedCornerShape(14.dp),
        backgroundBrush = RemoteBrushes.power,
        tint = RemoteColors.PowerRed,
        glowColor = RemoteColors.PowerRedGlow,
        hapticEnabled = haptic
    )

    @Composable
    fun dPad(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        haptic: Boolean = true,
        icon: ImageVector? = null,
        contentDescription: String? = null
    ) = RemoteButton(
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        contentDescription = contentDescription,
        shape = CircleShape,
        size = 48.dp,
        backgroundBrush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            center = Offset(0.5f, 0.5f),
            radius = 0.5f
        ),
        tint = RemoteColors.NeonCyan.copy(alpha = 0.7f),
        glowColor = RemoteColors.CyanGlow,
        hapticEnabled = haptic
    )

    @Composable
    fun dPadCenter(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        haptic: Boolean = true
    ) = RemoteButton(
        onClick = onClick,
        modifier = modifier,
        label = "OK",
        shape = CircleShape,
        size = 58.dp,
        fontSize = 11.sp,
        backgroundBrush = Brush.radialGradient(
            colors = listOf(Color(0xFF1A3A5C), Color(0xFF0A1F38)),
            center = Offset(0.4f, 0.3f),
            radius = 0.7f
        ),
        tint = RemoteColors.NeonCyan,
        glowColor = RemoteColors.CyanGlow,
        hapticEnabled = haptic
    )

    @Composable
    fun action(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        label: String = "",
        size: Dp = 54.dp,
        haptic: Boolean = true,
        icon: ImageVector? = null,
        contentDescription: String? = null
    ) = RemoteButton(
        onClick = onClick,
        modifier = modifier,
        label = label,
        icon = icon,
        contentDescription = contentDescription,
        size = size,
        fontSize = 9.sp,
        shape = RoundedCornerShape(14.dp),
        backgroundBrush = RemoteBrushes.glass,
        tint = RemoteColors.OnSurfaceMid,
        glowColor = RemoteColors.CyanGlow.copy(alpha = 0.3f),
        hapticEnabled = haptic
    )

    @Composable
    fun media(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector,
        haptic: Boolean = true
    ) = RemoteButton(
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        size = 48.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundBrush = RemoteBrushes.glass,
        tint = RemoteColors.NeonCyan,
        glowColor = RemoteColors.CyanGlow,
        hapticEnabled = haptic
    )

    @Composable
    fun numpad(
        digit: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        haptic: Boolean = true
    ) = RemoteButton(
        onClick = onClick,
        modifier = modifier,
        label = digit,
        size = 66.dp,
        fontSize = 18.sp,
        shape = RoundedCornerShape(14.dp),
        backgroundBrush = RemoteBrushes.glass,
        tint = RemoteColors.OnSurface,
        glowColor = RemoteColors.CyanGlow.copy(alpha = 0.1f),
        hapticEnabled = haptic
    )
}
