package com.tvhanan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interact.MutableInteractionSource
import androidx.compose.material3.ButtonDefaults
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.TextDim
import com.tvhanan.ui.theme.TextPrimary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun ScalableButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    shape: Shape = RoundedCornerShape(4.dp),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    scaleFactor: Float = 1.0
) {
    val enabledAlpha = if (enabled) 1.0f else 0.5f

    Button(
        onClick = { if (enabled) onClick() },
        enabled = enabled,
        modifier = modifier
            .scale(scaleFactor)
            .background(
                color = backgroundColor.copy(alpha = backgroundColor.alpha * enabledAlpha),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = borderColor.alpha * enabledAlpha),
                shape = shape
            )
            .padding(contentPadding),
        interactionSource = remember { MutableInteractionSource() },
        elevation = ButtonDefaults.elevation(
            defaultElevation = if (enabled) 2.dp else 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
        ) {
            content()
        }
    }
}