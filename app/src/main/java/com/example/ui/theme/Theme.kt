package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeMode

private val NutriLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = SageOnSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = AmberOnTertiaryContainer,
    background = CreamBackground,
    onBackground = InkOnSurface,
    surface = CreamSurface,
    onSurface = InkOnSurface,
    surfaceVariant = CreamSurfaceContainer,
    onSurfaceVariant = InkOnSurfaceVariant,
    surfaceContainerLowest = CreamSurfaceContainerLowest,
    surfaceContainerLow = CreamSurfaceContainerLow,
    surfaceContainer = CreamSurfaceContainer,
    surfaceContainerHigh = CreamSurfaceContainerHigh,
    surfaceContainerHighest = CreamSurfaceContainerHighest,
    outline = SubtleOutline,
    outlineVariant = SubtleOutlineVariant,
    error = ErrorRose
)

private val NutriDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageSecondaryContainerDark,
    onSecondaryContainer = SageOnSecondaryContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberTertiaryContainerDark,
    onTertiaryContainer = AmberOnTertiaryContainerDark,
    background = ForestBackgroundDark,
    onBackground = LightOnSurfaceDark,
    surface = ForestSurfaceDark,
    onSurface = LightOnSurfaceDark,
    surfaceVariant = ForestSurfaceContainerDark,
    onSurfaceVariant = LightOnSurfaceVariantDark,
    surfaceContainerLowest = ForestBackgroundDark,
    surfaceContainerLow = ForestSurfaceContainerLowDark,
    surfaceContainer = ForestSurfaceContainerDark,
    surfaceContainerHigh = ForestSurfaceContainerHighDark,
    surfaceContainerHighest = ForestSurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRoseDark
)

val NutriLifeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun NutriLifeTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) NutriDarkColorScheme else NutriLightColorScheme,
        typography = NutriLifeTypography,
        shapes = NutriLifeShapes,
        content = content
    )
}
