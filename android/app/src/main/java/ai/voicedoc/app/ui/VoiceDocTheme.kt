package ai.voicedoc.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object VoiceDocColors {
    val Forest = Color(0xFF176B4D)
    val Green = Color(0xFF25875F)
    val Mint = Color(0xFFDDF4E8)
    val PaleMint = Color(0xFFF1FAF5)
    val Ink = Color(0xFF18352A)
    val Muted = Color(0xFF718078)
    val Canvas = Color(0xFFF7FAF8)
    val Line = Color(0xFFE5EEE8)
    val White = Color(0xFFFFFFFF)
}

@Composable
internal fun VoiceDocTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = VoiceDocColors.Forest,
            onPrimary = VoiceDocColors.White,
            primaryContainer = VoiceDocColors.Mint,
            onPrimaryContainer = VoiceDocColors.Forest,
            secondary = VoiceDocColors.Green,
            onSecondary = VoiceDocColors.White,
            background = VoiceDocColors.Canvas,
            onBackground = VoiceDocColors.Ink,
            surface = VoiceDocColors.White,
            onSurface = VoiceDocColors.Ink,
            surfaceVariant = VoiceDocColors.PaleMint,
            onSurfaceVariant = VoiceDocColors.Muted,
            outline = VoiceDocColors.Line
        ),
        content = content
    )
}
