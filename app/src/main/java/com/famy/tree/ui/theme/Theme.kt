package com.famy.tree.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = TertiaryDark,
    error = Error,
    onError = OnError,
    errorContainer = ErrorLight,
    onErrorContainer = Error,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = SurfaceDark,
    inverseOnSurface = OnSurfaceDark,
    inversePrimary = PrimaryLight,
    surfaceTint = Primary,
    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = PrimaryDark,
    primaryContainer = Primary,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryLight,
    onSecondary = SecondaryDark,
    secondaryContainer = Secondary,
    onSecondaryContainer = SecondaryLight,
    tertiary = TertiaryLight,
    onTertiary = TertiaryDark,
    tertiaryContainer = Tertiary,
    onTertiaryContainer = TertiaryLight,
    error = ErrorLight,
    onError = Error,
    errorContainer = Error,
    onErrorContainer = ErrorLight,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = SurfaceLight,
    inverseOnSurface = OnSurfaceLight,
    inversePrimary = Primary,
    surfaceTint = PrimaryLight,
    scrim = Color.Black
)

@Composable
fun FamyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            if (activity is ComponentActivity) {
                activity.enableEdgeToEdge()
            }
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FamyTypography,
        content = content
    )
}

object FamyThemeExtensions {
    @Composable
    fun memberCardColor(
        gender: com.famy.tree.domain.model.Gender,
        darkTheme: Boolean = isSystemInDarkTheme()
    ): Color {
        return when (gender) {
            com.famy.tree.domain.model.Gender.MALE -> if (darkTheme) MaleCardColorDark else MaleCardColor
            com.famy.tree.domain.model.Gender.FEMALE -> if (darkTheme) FemaleCardColorDark else FemaleCardColor
            com.famy.tree.domain.model.Gender.OTHER -> if (darkTheme) OtherCardColorDark else OtherCardColor
            com.famy.tree.domain.model.Gender.UNKNOWN -> if (darkTheme) UnknownCardColorDark else UnknownCardColor
        }
    }

    fun generationColor(generation: Int): Color {
        return when {
            generation <= 0 -> Generation0Color
            generation == 1 -> Generation1Color
            generation == 2 -> Generation2Color
            generation == 3 -> Generation3Color
            generation == 4 -> Generation4Color
            generation == 5 -> Generation5Color
            else -> Generation6PlusColor
        }
    }

    fun relationshipLineColor(
        type: com.famy.tree.domain.model.RelationshipKind
    ): Color {
        return when (type) {
            com.famy.tree.domain.model.RelationshipKind.PARENT,
            com.famy.tree.domain.model.RelationshipKind.CHILD -> PaternalLineColor
            com.famy.tree.domain.model.RelationshipKind.SPOUSE,
            com.famy.tree.domain.model.RelationshipKind.EX_SPOUSE -> SpouseLineColor
            com.famy.tree.domain.model.RelationshipKind.SIBLING -> SiblingLineColor
        }
    }
}
