package com.tvhanan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.GlassBorderStrong
import com.tvhanan.ui.theme.GlassSurface
import com.tvhanan.ui.theme.GlassSurfacePressed
import com.tvhanan.ui.theme.TextPrimary

/**
 * Tombol "kaca" (glassmorphism) — pengganti RemoteButton/FilledIconButton.
 *
 * Kenapa custom, bukan FilledIconButton bawaan Material:
 * - Ripple effect Material default tidak cocok dengan gaya scale-press
 *   yang sudah disepakati di preview (tombol "mengecil" saat ditekan,
 *   bukan beriak warna).
 * - Butuh kontrol penuh atas background (bisa transparan-glass ATAU
 *   gradient solid utk tombol aksen seperti Power), yang lebih mudah
 *   dikontrol lewat composable sendiri.
 *
 * indication = null secara sengaja mematikan ripple bawaan; feedback
 * visual sepenuhnya dari animasi scale + perubahan warna background.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    gradientColors: List<Color>? = null,
    borderColor: Color = GlassBorder,
    contentColor: Color = TextPrimary,
    enabled: Boolean = true,
    onPressedChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // visualPressed dipaksa bertahan minimal ~120ms walau isPressed asli
    // sudah balik ke false lebih cepat (tap cepat) — supaya animasi scale
    // SELALU sempat terlihat minimal 1-2 frame, tidak "ketelan" oleh
    // tap-release yang lebih cepat dari waktu render.
    var visualPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            visualPressed = true
            onPressedChange?.invoke(true)
        } else {
            kotlinx.coroutines.delay(110)
            visualPressed = false
            onPressedChange?.invoke(false)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (visualPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 70, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "glassButtonScale"
    )

    val backgroundModifier = if (gradientColors != null) {
        Modifier.background(Brush.linearGradient(gradientColors), shape)
    } else {
        Modifier.background(if (visualPressed) GlassSurfacePressed else GlassSurface, shape)
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .scale(scale)
            .then(backgroundModifier)
            .border(1.dp, if (visualPressed) GlassBorderStrong else borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
