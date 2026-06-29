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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    autoRepeat: Boolean = false, // Parameter baru untuk kontrol auto-repeat
    onPressedChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

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

    // Pilih modifier klik secara dinamis berdasarkan parameter autoRepeat
    val clickModifier = if (autoRepeat) {
        Modifier.repeatingClickable(
            interactionSource = interactionSource,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(backgroundModifier)
            .border(1.dp, if (visualPressed) GlassBorderStrong else borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

// Ekstensi helper modifier baru untuk menangani penekanan berulang secara aman
fun Modifier.repeatingClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.pointerInput(interactionSource, enabled) {
    if (!enabled) return@pointerInput
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val press = PressInteraction.Press(down.position)
            
            // Emulasikan sentuhan ke interaction source agar tombol mengecil secara visual
            launch {
                interactionSource.emit(press)
            }
            
           // Luncurkan perulangan klik di coroutine terpisah (Tuned to Physical Remote Standards)
            val repeatJob = launch {
                onClick()  // Klik pertama langsung ditembak instan tanpa jeda (0ms)
                delay(300) // Jeda penahanan awal (300ms) sebelum mulai mengulang secara otomatis
                while (true) {
                    onClick()
                    delay(100) // Mengirim klik setiap 100ms (10 klik per detik - Respons instan & aman)
                }
            }
            
            // Tunggu hingga jari diangkat atau ditarik keluar bounds
            val up = waitForUpOrCancellation()
            
            // Batalkan loop pengulangan klik seketika
            repeatJob.cancel()
            
            // Lepaskan status visual tombol kembali normal
            launch {
                if (up != null) {
                    interactionSource.emit(PressInteraction.Release(press))
                } else {
                    interactionSource.emit(PressInteraction.Cancel(press))
                }
            }
        }
    }
}
