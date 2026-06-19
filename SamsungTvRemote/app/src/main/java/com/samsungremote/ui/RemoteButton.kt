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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/**
 * A stylized remote-control button with haptic feedback, press
 * animation, and a glass-morphism look.
 *
 * @param hapticEnabled when true, a short 30 ms vibration fires on press.
 */
@Composable
fun RemoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    icon: ImageVector? = null,
    painter: Painter? = null,
    contentDescription: String? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    size: Dp = 60.dp,
    fontSize: TextUnit = 10.sp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    backgroundBrush: Brush = Brush.linearGradient(
        listOf(RemoteColors.ButtonGradientStart, RemoteColors.ButtonGradientEnd)
    ),
    borderColor: Color = RemoteColors.ButtonBorder,
    hapticEnabled: Boolean = true,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(if (enabled) 4.dp else 0.dp, shape, ambientColor = RemoteColors.GlassShadow)
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
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription ?: label,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(size * 0.45f)
            )
        } else if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription ?: label,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(size * 0.45f)
            )
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = if (enabled) tint else tint.copy(alpha = 0.38f),
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // Glass edge highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(RemoteColors.GlassHighlight)
        )
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
        vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {
        // Haptic is best-effort
    }
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
        label = "POWER",
        size = 64.dp,
        fontSize = 11.sp,
        shape = RoundedCornerShape(14.dp),
        backgroundBrush = Brush.linearGradient(
            listOf(RemoteColors.PowerRed, RemoteColors.PowerRed.copy(alpha = 0.7f))
        ),
        tint = Color.White,
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
        size = 52.dp,
        backgroundBrush = Brush.linearGradient(
            listOf(RemoteColors.SurfaceElevated, RemoteColors.SurfaceVariant)
        ),
        tint = RemoteColors.NeonCyan,
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
        size = 48.dp,
        fontSize = 12.sp,
        backgroundBrush = Brush.linearGradient(
            listOf(RemoteColors.NeonCyan, RemoteColors.NeonCyan.copy(alpha = 0.7f))
        ),
        tint = RemoteColors.Background,
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
        size = 64.dp,
        fontSize = 20.sp,
        shape = RoundedCornerShape(16.dp),
        backgroundBrush = Brush.linearGradient(
            listOf(RemoteColors.SurfaceVariant, RemoteColors.SurfaceElevated)
        ),
        tint = RemoteColors.OnSurface,
        borderColor = RemoteColors.ButtonBorder,
        hapticEnabled = haptic
    )
}
