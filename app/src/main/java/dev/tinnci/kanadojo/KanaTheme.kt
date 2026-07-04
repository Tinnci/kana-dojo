package dev.tinnci.kanadojo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun KanaTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF2F5D50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCEBDD),
        onPrimaryContainer = Color(0xFF0E2B23),
        secondary = Color(0xFFA66A5A),
        secondaryContainer = Color(0xFFFFDFD6),
        tertiary = Color(0xFF6B5CA5),
        tertiaryContainer = Color(0xFFE7DEFF),
        background = Color(0xFFFFF8F0),
        surface = Color(0xFFFFFCF7),
        surfaceVariant = Color(0xFFF1E6D6),
        outlineVariant = Color(0xFFD8C8B7)
    )

    MaterialTheme(colorScheme = colorScheme, content = content)
}
