package com.tvhanan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvhanan.ui.theme.GlassBorder
import com.tvhanan.ui.theme.GlassSurface
import com.tvhanan.ui.theme.NavAccent
import com.tvhanan.ui.theme.NavAccent2
import com.tvhanan.ui.theme.TextPrimary

@Composable
fun DpadSection(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(216.dp)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        NavAccent.copy(alpha = 0.55f),
                        NavAccent2.copy(alpha = 0.55f),
                        NavAccent.copy(alpha = 0.55f)
                    )
                ),
                CircleShape
            )
            .padding(3.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF14161C), Color(0xFF0D0E12))
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        DpadArrow(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
            text = "\u25B2",
            onClick = onUp
        )
        DpadArrow(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            text = "\u25BC",
            onClick = onDown
        )
        DpadArrow(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
            text = "\u25C0",
            onClick = onLeft
        )
        DpadArrow(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
            text = "\u25B6",
            onClick = onRight
        )

        GlassButton(
            onClick = onOk,
            modifier = Modifier.size(74.dp),
            gradient = listOf(NavAccent.copy(alpha = 0.20f), NavAccent2.copy(alpha = 0.18f)),
            borderColor = NavAccent.copy(alpha = 0.45f),
            cornerRadius = 50
        ) {
            Text(
                text = "OK",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.03.sp
            )
        }
    }
}

@Composable
private fun BoxScope.DpadArrow(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    GlassButton(
        onClick = onClick,
        modifier = modifier.size(50.dp),
        cornerRadius = 15,
        borderColor = Color.Transparent,
        hapticFeedback = true
    ) {
        Text(
            text = text,
            color = TextPrimary.copy(alpha = 0.85f),
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}
