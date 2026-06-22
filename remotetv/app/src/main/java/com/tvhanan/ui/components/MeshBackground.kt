package com.tvhanan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.tvhanan.ui.theme.BgBase
import com.tvhanan.ui.theme.MeshBlob1
import com.tvhanan.ui.theme.MeshBlob2
import com.tvhanan.ui.theme.MeshBlob3
import com.tvhanan.ui.theme.MeshBlob4

@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(BgBase)) {
        drawMeshBlob(MeshBlob1, Offset(size.width * 0.10f, size.height * 0.04f), size.width * 0.55f, alpha = 0.50f)
        drawMeshBlob(MeshBlob2, Offset(size.width * 0.95f, size.height * 0.22f), size.width * 0.50f, alpha = 0.40f)
        drawMeshBlob(MeshBlob3, Offset(size.width * 0.08f, size.height * 0.55f), size.width * 0.55f, alpha = 0.34f)
        drawMeshBlob(MeshBlob4, Offset(size.width * 0.90f, size.height * 0.72f), size.width * 0.50f, alpha = 0.36f)
    }
}

private fun DrawScope.drawMeshBlob(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
