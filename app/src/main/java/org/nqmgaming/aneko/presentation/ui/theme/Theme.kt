package org.nqmgaming.aneko.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AccentColor(val key: String, val displayColor: Color) {
    Coral("coral", CoralPrimary),
    Blue("blue", BluePrimary),
    Green("green", GreenPrimary),
    Purple("purple", PurplePrimary),
    Teal("teal", TealPrimary),
    Amber("amber", AmberPrimary),
    Rose("rose", RosePrimary),
    Indigo("indigo", IndigoPrimary);

    companion object {
        fun fromKey(key: String): AccentColor =
            entries.firstOrNull { it.key == key } ?: Coral
    }
}

private fun lightSchemeFor(accent: AccentColor) = when (accent) {
    AccentColor.Coral -> lightColorScheme(
        primary = CoralPrimary, secondary = CoralSecondary, tertiary = CoralTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = White, onTertiary = White,
        onBackground = DarkGray, onSurface = DarkGray
    )

    AccentColor.Blue -> lightColorScheme(
        primary = BluePrimary, secondary = BlueSecondary, tertiary = BlueTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = White, onTertiary = DarkGray,
        onBackground = DarkGray, onSurface = DarkGray
    )

    AccentColor.Green -> lightColorScheme(
        primary = GreenPrimary, secondary = GreenSecondary, tertiary = GreenTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = White, onTertiary = DarkGray,
        onBackground = DarkGray, onSurface = DarkGray
    )

    AccentColor.Purple -> lightColorScheme(
        primary = PurplePrimary, secondary = PurpleSecondary, tertiary = PurpleTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = White, onTertiary = DarkGray,
        onBackground = DarkGray, onSurface = DarkGray
    )

    AccentColor.Teal -> lightColorScheme(
        primary = TealPrimary, secondary = TealSecondary, tertiary = TealTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = White, onTertiary = DarkGray,
        onBackground = DarkGray, onSurface = DarkGray
    )

    AccentColor.Amber -> lightColorScheme(
        primary = AmberPrimary, secondary = AmberSecondary, tertiary = AmberTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = DarkGray, onSurface = DarkGray
    )

    AccentColor.Rose -> lightColorScheme(
        primary = RosePrimary, secondary = RoseSecondary, tertiary = RoseTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = White, onTertiary = DarkGray,
        onBackground = DarkGray, onSurface = DarkGray
    )

    AccentColor.Indigo -> lightColorScheme(
        primary = IndigoPrimary, secondary = IndigoSecondary, tertiary = IndigoTertiary,
        background = WhiteSmoke, surface = White,
        onPrimary = White, onSecondary = White, onTertiary = DarkGray,
        onBackground = DarkGray, onSurface = DarkGray
    )
}

private fun darkSchemeFor(accent: AccentColor) = when (accent) {
    AccentColor.Coral -> darkColorScheme(
        primary = CoralPrimaryDark, secondary = CoralSecondaryDark, tertiary = CoralTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )

    AccentColor.Blue -> darkColorScheme(
        primary = BluePrimaryDark, secondary = BlueSecondaryDark, tertiary = BlueTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )

    AccentColor.Green -> darkColorScheme(
        primary = GreenPrimaryDark, secondary = GreenSecondaryDark, tertiary = GreenTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )

    AccentColor.Purple -> darkColorScheme(
        primary = PurplePrimaryDark, secondary = PurpleSecondaryDark, tertiary = PurpleTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )

    AccentColor.Teal -> darkColorScheme(
        primary = TealPrimaryDark, secondary = TealSecondaryDark, tertiary = TealTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )

    AccentColor.Amber -> darkColorScheme(
        primary = AmberPrimaryDark, secondary = AmberSecondaryDark, tertiary = AmberTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )

    AccentColor.Rose -> darkColorScheme(
        primary = RosePrimaryDark, secondary = RoseSecondaryDark, tertiary = RoseTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )

    AccentColor.Indigo -> darkColorScheme(
        primary = IndigoPrimaryDark, secondary = IndigoSecondaryDark, tertiary = IndigoTertiaryDark,
        background = AlmostBlack, surface = DarkGraySurface,
        onPrimary = DarkGray, onSecondary = DarkGray, onTertiary = DarkGray,
        onBackground = LightGray, onSurface = LightGray
    )
}

@Composable
fun ANekoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    accentColor: AccentColor = AccentColor.Coral,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkSchemeFor(accentColor)
        else -> lightSchemeFor(accentColor)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}