package com.antest1.gotobrowser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Color schemes for the settings sheet and the in-game status overlay (loading card and
// server-error snackbar, always dark). The game controls (toolbar, FAB, glassy buttons)
// keep the neutral GotobrowserTheme so they stay unobtrusive over the game.
//
// Generated from seed #005FAC (the seed ReVanced Manager derives its palette from) with
// the Material "tonal spot" variant, the same way Material Theme Builder / Material You
// derive a palette. Every role is filled in so containers, outlines and elevated surfaces
// are tinted consistently; leaving roles unset falls back to the purple Material baseline.
//
// Regenerate with material-color-utilities if the seed changes; do not hand-edit tones.

// Warning accent of the status overlay. M3 has no warning role, so this is a custom color
// in HCT: the hue of #FFC400 at tone 80 like the dark roles below, with the chroma of the
// dark error (#FFB4AB, chroma 29.4) so the two status colors read at the same strength.
// It is not harmonized toward the seed, which would shift it to olive.
val StatusWarningColor = Color(0xFFE2C37E)

private val SettingsDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5C9FE),
    onPrimary = Color(0xFF00315D),
    primaryContainer = Color(0xFF204876),
    onPrimaryContainer = Color(0xFFD4E3FF),
    inversePrimary = Color(0xFF3B608F),
    secondary = Color(0xFFBCC7DB),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFD9BDE3),
    onTertiary = Color(0xFF3D2946),
    tertiaryContainer = Color(0xFF543F5E),
    onTertiaryContainer = Color(0xFFF6D9FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE1E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE1E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    surfaceTint = Color(0xFFA5C9FE),
    inverseSurface = Color(0xFFE1E2E9),
    inverseOnSurface = Color(0xFF2E3035),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF37393E),
    surfaceDim = Color(0xFF111318),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainerLowest = Color(0xFF0C0E13)
)

private val SettingsLightColorScheme = lightColorScheme(
    primary = Color(0xFF3B608F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF204876),
    inversePrimary = Color(0xFFA5C9FE),
    secondary = Color(0xFF545F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF3D4758),
    tertiary = Color(0xFF6D5677),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6D9FF),
    onTertiaryContainer = Color(0xFF543F5E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF3B608F),
    inverseSurface = Color(0xFF2E3035),
    inverseOnSurface = Color(0xFFF0F0F7),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF8F9FF),
    surfaceDim = Color(0xFFD9DAE0),
    surfaceContainer = Color(0xFFEDEDF4),
    surfaceContainerHigh = Color(0xFFE7E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E9),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainerLowest = Color(0xFFFFFFFF)
)

/** The dark scheme's error, for status accents drawn outside a SettingsTheme (the refresh FAB ring). */
val StatusErrorColor: Color get() = SettingsDarkColorScheme.error

/** Wraps the settings sheet in the blue palette, overriding [GotobrowserTheme] beneath it. */
@Composable
fun SettingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SettingsDarkColorScheme else SettingsLightColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
