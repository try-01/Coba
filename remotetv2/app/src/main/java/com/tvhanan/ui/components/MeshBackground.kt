package com.tvhanan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

/**
 * Background "aurora" statis — beberapa radial gradient blob yang
 * di-blend di atas base gelap. Sengaja TIDAK animasi dan TIDAK
 * memakai BlurEffect/backdrop-blur real-time, karena:
 *
 * 1. Canvas ini hanya digambar ulang saat ukurannya berubah (rotasi
 *    layar/resize), bukan setiap frame atau setiap recomposition lain
 *    di layar (klik tombol dsb tidak memicu redraw blob ini).
 * 2. BlurEffect real-time mahal di GPU low-end (penting untuk target
 *    Android 9 / device lawas) — di sini efek "kaca" disimulasikan
 *    lewat alpha pada komponen GlassButton, bukan blur sungguhan.
 *
 * Dipasang sebagai layer paling belakang (di belakang konten scroll),
 * mengisi seluruh layar via Modifier.fillMaxSize() dari pemanggil.
 * 
 * Menggunakan remember untuk caching komposisi draw, menghindari
 * redraw berulang saat recomposition komponen lain.
 */
@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    val cachedDraw = remember { MeshDrawCache() }
    
    Canvas(
        modifier = modifier.background(BgBase)
    ) {
        cachedDraw.drawMesh(this)
    }
}

private class MeshDrawCache {
    private var lastSize: androidx.compose.ui.unit.IntSize? = null
    private var cachedCommands: (DrawScope.() -> Unit)? = null
    
    fun drawMesh(drawScope: DrawScope) {
        val currentSize = drawScope.size
        if (lastSize != currentSize || cachedCommands == null) {
            lastSize = currentSize
            cachedCommands = {
                drawMeshBlob(
                    color = MeshBlob1,
                    center = Offset(currentSize.width * 0.08f, currentSize.height * 0.05f),
                    radius = currentSize.width * 0.62f,
                    alpha = 0.65f
                )
                drawMeshBlob(
                    color = MeshBlob2,
                    center = Offset(currentSize.width * 0.96f, currentSize.height * 0.24f),
                    radius = currentSize.width * 0.58f,
                    alpha = 0.55f
                )
                drawMeshBlob(
                    color = MeshBlob3,
                    center = Offset(currentSize.width * 0.10f, currentSize.height * 0.56f),
                    radius = currentSize.width * 0.62f,
                    alpha = 0.48f
                )
                drawMeshBlob(
                    color = MeshBlob4,
                    center = Offset(currentSize.width * 0.92f, currentSize.height * 0.74f),
                    radius = currentSize.width * 0.58f,
                    alpha = 0.50f
                )
            }
        }
        cachedCommands?.invoke(drawScope)
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
}
