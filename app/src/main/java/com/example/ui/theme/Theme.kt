package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDeepBlue,
    onPrimary = PureWhite,
    primaryContainer = PrimaryBlue100,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = PrimaryBlueLight,
    onSecondary = PureWhite,
    secondaryContainer = PrimaryBlue50,
    onSecondaryContainer = PrimaryDeepBlue,
    tertiary = SuccessGreen,
    onTertiary = PureWhite,
    tertiaryContainer = SuccessGreenBg,
    onTertiaryContainer = SuccessGreen,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = Slate100,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = Slate200
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlue100,
    secondary = PrimaryBlueLight,
    onSecondary = TextPrimary,
    secondaryContainer = PrimaryDeepBlue,
    onSecondaryContainer = PrimaryBlue50,
    tertiary = SuccessGreen,
    onTertiary = PureWhite,
    tertiaryContainer = SuccessGreenBg,
    onTertiaryContainer = PureWhite,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed,
    background = BackgroundColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = Slate100,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = Slate200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Explicit Light Theme to prevent any dark inversion / white-on-white text issues
    dynamicColor: Boolean = false, // Cohesive brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun SahayakTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
