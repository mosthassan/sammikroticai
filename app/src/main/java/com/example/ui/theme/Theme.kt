package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val SamDarkColorScheme = darkColorScheme(
    primary = BrandCyanPrimary,
    onPrimary = Color(0xFF090D16),
    primaryContainer = Color(0xFF0C243C),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = BrandBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF132A4A),
    onSecondaryContainer = Color(0xFFE0F2FE),
    background = CyberDarkCanvas,
    onBackground = TextPrimaryDark,
    surface = CyberDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberDarkCardElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberBorder,
    outlineVariant = CyberBorderSubtle,
    error = SemanticExpenseRed,
    onError = Color.White,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFFD1DC)
)

private val SamLightColorScheme = lightColorScheme(
    primary = BrandCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = BrandBlueSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightSurfaceBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = SemanticExpenseRed,
    onError = Color.White
)

@Composable
fun SamMikrotikTheme(
    darkTheme: Boolean = true, // Dark theme is default for high-tech ISP operations
    layoutDirection: LayoutDirection = LayoutDirection.Rtl, // Arabic RTL first
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SamDarkColorScheme else SamLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
