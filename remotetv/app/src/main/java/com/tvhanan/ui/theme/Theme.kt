package com.tvhanan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NavAccent,
    secondary = NavAccent2,
    tertiary = AccentWarn,
    background = BgBase,
    surface = GlassSurface,
    surfaceVariant = GlassSurface,
    error = DisconnectedColor
)

@Composable
fun TvRemoteTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RemoteTypography,
        content = content
    )
}
