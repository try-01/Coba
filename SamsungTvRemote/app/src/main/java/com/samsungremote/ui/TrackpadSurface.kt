package com.samsungremote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * A large touch-sensitive surface that mimics a TV trackpad.
 *
 * - **Tap** → triggers [onClick] (sends ENTER / select).
 * - **Swipe** (≥ 30 px threshold) → triggers [onSwipe] with the
 *   dominant direction so the ViewModel can send the corresponding
 *   D-Pad key. Repeated direction calls are debounced naturally by
 *   the gesture system.
 */
@Composable
fun TrackpadSurface(
    onSwipe: (direction: SwipeDirection) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        RemoteColors.SurfaceVariant,
                        RemoteColors.SurfaceElevated
                    )
                )
            )
            .border(1.dp, RemoteColors.ButtonBorder, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onClick() }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        val start = down.changes.first().position

                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()
                            val delta = change.position - start

                            when {
                                abs(delta.x) > 30f && abs(delta.x) > abs(delta.y) -> {
                                    if (delta.x > 0) onSwipe(SwipeDirection.RIGHT)
                                    else onSwipe(SwipeDirection.LEFT)
                                    // Reset start to avoid repeated triggers
                                    start += change.position - start
                                }
                                abs(delta.y) > 30f && abs(delta.y) > abs(delta.x) -> {
                                    if (delta.y > 0) onSwipe(SwipeDirection.DOWN)
                                    else onSwipe(SwipeDirection.UP)
                                    start += change.position - start
                                }
                            }
                            change.consume()
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Swipe to navigate D-Pad\nTap to select",
            color = RemoteColors.OnSurfaceDim.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }
