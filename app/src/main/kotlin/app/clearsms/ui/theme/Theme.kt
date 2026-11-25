// Modified for Inbo from Clear SMS. See LICENSE and NOTICE.

package app.clearsms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import app.clearsms.R
import app.clearsms.domain.model.ThemeMode

@Composable
private fun inboColorScheme(dark: Boolean): ColorScheme {
    val mint = colorResource(R.color.inbo_mint)
    val forest = colorResource(R.color.inbo_forest)
    val apricot = colorResource(R.color.inbo_apricot)
    val paper = colorResource(R.color.inbo_paper)
    val ink = colorResource(R.color.inbo_ink)

    return remember(dark, mint, forest, apricot, paper, ink) {
        if (dark) {
            darkColorScheme(
                primary = mint,
                onPrimary = forest,
                primaryContainer = forest,
                onPrimaryContainer = mint,
                inversePrimary = forest,
                secondary = Color(0xFFB3CABA),
                onSecondary = Color(0xFF24392F),
                secondaryContainer = Color(0xFF304339),
                onSecondaryContainer = paper,
                tertiary = apricot,
                onTertiary = Color(0xFF482C06),
                tertiaryContainer = Color(0xFF573714),
                onTertiaryContainer = apricot,
                background = ink,
                onBackground = paper,
                surface = ink,
                onSurface = paper,
                surfaceVariant = Color(0xFF33443A),
                onSurfaceVariant = Color(0xFFB9C9BD),
                surfaceTint = mint,
                surfaceDim = Color(0xFF131A16),
                surfaceBright = Color(0xFF344238),
                surfaceContainerLowest = Color(0xFF111713),
                surfaceContainerLow = Color(0xFF202A24),
                surfaceContainer = Color(0xFF253129),
                surfaceContainerHigh = Color(0xFF2D3A31),
                surfaceContainerHighest = Color(0xFF36473C),
                inverseSurface = paper,
                inverseOnSurface = ink,
                outline = Color(0xFF8FA198),
                outlineVariant = Color(0xFF526359),
            )
        } else {
            lightColorScheme(
                primary = forest,
                onPrimary = paper,
                primaryContainer = mint,
                onPrimaryContainer = forest,
                inversePrimary = mint,
                secondary = Color(0xFF455B4F),
                onSecondary = paper,
                secondaryContainer = Color(0xFFDFEAE2),
                onSecondaryContainer = ink,
                tertiary = Color(0xFF704709),
                onTertiary = paper,
                tertiaryContainer = apricot,
                onTertiaryContainer = ink,
                background = paper,
                onBackground = ink,
                surface = paper,
                onSurface = ink,
                surfaceVariant = Color(0xFFE2E9E0),
                onSurfaceVariant = Color(0xFF46564D),
                surfaceTint = forest,
                surfaceDim = Color(0xFFD7DFD6),
                surfaceBright = paper,
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFEEF2EA),
                surfaceContainer = Color(0xFFE8EEE5),
                surfaceContainerHigh = Color(0xFFE1E9E0),
                surfaceContainerHighest = Color(0xFFDCE6DC),
                inverseSurface = Color(0xFF28332D),
                inverseOnSurface = paper,
                outline = Color(0xFF64746A),
                outlineVariant = Color(0xFFBDCBBF),
            )
        }
    }
}

/** The Inbo palette is the default, with optional wallpaper colors on Android 12+. */
@Composable
fun ClearSmsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            inboColorScheme(darkTheme)
        }
    val semanticAmountColors = if (darkTheme) SemanticAmountColors.Dark else SemanticAmountColors.Light
    CompositionLocalProvider(LocalSemanticAmountColors provides semanticAmountColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ClearSmsTypography,
            shapes = ClearSmsShapes,
            content = content,
        )
    }
}
