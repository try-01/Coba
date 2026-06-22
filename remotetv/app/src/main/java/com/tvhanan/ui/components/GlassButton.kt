package com.tvhanan.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.GlassBorderStrong
import com.tvhanan.ui.theme.GlassSurface
import com.tvhanan.ui.theme.GlassSurfacePressed
import com.tvhanan.util.HapticUtil

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 18,
    gradient: List<Color>? = null,
    borderColor: Color = GlassBorder,
    hapticFeedback: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(90),
        label = "glassBtnScale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed && hapticFeedback) HapticUtil.tick()
    }

    val shape = RoundedCornerShape(cornerRadius.dp)
    val bgMod = when {
        gradient != null -> Modifier.background(
            Brush.linearGradient(gradient), shape
        )
        isPressed -> Modifier.background(GlassSurfacePressed, shape)
        else -> Modifier.background(GlassSurface, shape)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(bgMod)
            .border(
                width = 1.dp,
                color = if (isPressed) GlassBorderStrong else borderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
