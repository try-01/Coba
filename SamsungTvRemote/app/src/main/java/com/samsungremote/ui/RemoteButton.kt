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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
            isPressed -> 0.92f
            else -> 1f
        },
        label = "pressScale"
    )

    val animElevation by animateFloatAsState(
        targetValue = when {
            !enabled -> 0f
            isPressed -> 2f
            else -> 8f
        },
        label = "elevation"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(animScale)
            .shadow(
                elevation = animElevation.dp,
                shape = shape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(shape)
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
        // Inner glass highlight (top edge shine)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(RemoteColors.ButtonHighlight)
        )

        // Glow ring (for power/accent buttons)
        if (glowColor != Color.Transparent && enabled) {
            Box(
                modifier = Modifier
                    .size(size * 0.85f)
                    .clip(shape)
                    .background(glowColor.copy(alpha = 0.15f))
            )
        }

        // Content
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
                fontWeight = FontWeight.Medium,
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

// ── Presets ──────────────────────────────────────────────────

object ButtonPresets {
    @Composable
    fun power(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        haptic: Boolean = true
    ) = RemoteButton(
        onClick = onClick,
        modifier = modifier,
        icon = androidx.compose.material.icons.Icons.Filled.PowerSettingsNew,
        contentDescription = "Power",
        size = 68.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundBrush = RemoteBrushes.power,
        tint = Color.White,
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
        size = 54.dp,
        backgroundBrush = RemoteBrushes.glass,
        tint = RemoteColors.NeonCyan,
        glowColor = RemoteColors.NeonCyanGlow,
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
        size = 50.dp,
        fontSize = 13.sp,
        backgroundBrush = RemoteBrushes.accent,
        tint = RemoteColors.BackgroundDeep,
        glowColor = RemoteColors.NeonCyanGlow,
        hapticEnabled = haptic
    )

    @Composable
    fun action(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        label: String = "",
        size: Dp = 56.dp,
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
        fontSize = 10.sp,
        shape = RoundedCornerShape(14.dp),
        tint = RemoteColors.OnSurface,
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
        fontSize = 22.sp,
        shape = RoundedCornerShape(16.dp),
        backgroundBrush = RemoteBrushes.glass,
        tint = RemoteColors.OnSurface,
        glowColor = RemoteColors.NeonCyanGlow.copy(alpha = 0.1f),
        hapticEnabled = haptic
    )
}
