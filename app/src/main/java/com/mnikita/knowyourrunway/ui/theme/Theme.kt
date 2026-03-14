package com.mnikita.knowyourrunway.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.mnikita.knowyourrunway.data.AccentPreset
import com.mnikita.knowyourrunway.data.ThemeMode

private data class AccentSet(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

// ✅ Luxury shapes (global)
private val NYRShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private fun accentSet(preset: AccentPreset): AccentSet {
    return when (preset) {
        AccentPreset.COFFEE -> AccentSet(
            primary = AccentOrange,
            secondary = CoffeeLight,
            tertiary = CoffeeDark
        )
        AccentPreset.RED -> AccentSet(
            primary = Color(0xFFE53935),
            secondary = Color(0xFFFFCDD2),
            tertiary = Color(0xFFB71C1C)
        )
        AccentPreset.BLUE -> AccentSet(
            primary = Color(0xFF1E88E5),
            secondary = Color(0xFFBBDEFB),
            tertiary = Color(0xFF0D47A1)
        )
        AccentPreset.PURPLE -> AccentSet(
            primary = Color(0xFF8E24AA),
            secondary = Color(0xFFE1BEE7),
            tertiary = Color(0xFF4A148C)
        )
        AccentPreset.GREEN -> AccentSet(
            primary = Color(0xFF43A047),
            secondary = Color(0xFFC8E6C9),
            tertiary = Color(0xFF1B5E20)
        )
        AccentPreset.PINK -> AccentSet(
            primary = Color(0xFFD81B60),
            secondary = Color(0xFFF8BBD0),
            tertiary = Color(0xFF880E4F)
        )
    }
}

private fun lightScheme(accent: AccentSet) = lightColorScheme(
    primary = accent.primary,
    onPrimary = Color.White,

    background = BlackBg,
    onBackground = TextMain,

    surface = SurfaceWhite,
    onSurface = TextMain,

    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextSub,

    outline = OutlineSoft,

    secondary = accent.secondary,
    onSecondary = accent.tertiary,

    tertiary = accent.tertiary,
    onTertiary = Color.White,

    error = Color(0xFFB3261E),
    onError = Color.White
)

private fun darkScheme(accent: AccentSet) = darkColorScheme(
    primary = accent.primary,
    onPrimary = Color.White,

    background = Color(0xFF0F0F10),
    onBackground = Color(0xFFEDEDED),

    surface = Color(0xFF161618),
    onSurface = Color(0xFFEDEDED),

    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFFB8B8C2),

    outline = Color(0xFF3A3A42),

    secondary = accent.secondary.copy(alpha = 0.35f),
    onSecondary = Color(0xFFEDEDED),

    tertiary = accent.tertiary,
    onTertiary = Color.White,

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun NYRTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPreset: AccentPreset = AccentPreset.COFFEE,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val accent = accentSet(accentPreset)

    MaterialTheme(
        colorScheme = if (dark) darkScheme(accent) else lightScheme(accent),
        typography = NYRTypography,
        shapes = NYRShapes,
        content = content
    )
}