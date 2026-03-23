package me.anyang.easyprint.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val iOSBlue = Color(0xFF007AFF)
private val iOSBlueLight = Color(0xFF5AC8FA)
private val iOSGreen = Color(0xFF34C759)
private val iOSRed = Color(0xFFFF3B30)
private val iOSOrange = Color(0xFFFF9500)
private val iOSYellow = Color(0xFFFFCC00)
private val iOSPurple = Color(0xFFAF52DE)
private val iOSPink = Color(0xFFFF2D55)
private val iOSTeal = Color(0xFF5AC8FA)
private val iOSGray = Color(0xFF8E8E93)
private val iOSGray2 = Color(0xFFAEAEB2)
private val iOSGray3 = Color(0xFFC7C7CC)
private val iOSGray4 = Color(0xFFD1D1D6)
private val iOSGray5 = Color(0xFFE5E5EA)
private val iOSGray6 = Color(0xFFF2F2F7)
private val iOSGrayDark = Color(0xFF636366)
private val iOSGrayDark2 = Color(0xFF48484A)
private val iOSGrayDark3 = Color(0xFF3A3A3C)
private val iOSGrayDark4 = Color(0xFF2C2C2E)
private val iOSGrayDark5 = Color(0xFF1C1C1E)

private val Primary10 = Color(0xFF001D36)
private val Primary20 = Color(0xFF003258)
private val Primary30 = Color(0xFF00497D)
private val Primary40 = iOSBlue
private val Primary50 = Color(0xFF1E7BB8)
private val Primary60 = Color(0xFF4295CE)
private val Primary70 = Color(0xFF63B0EA)
private val Primary80 = Color(0xFF9CCBFF)
private val Primary90 = Color(0xFFD0E4FF)
private val Primary95 = Color(0xFFEFF4FF)
private val Primary99 = Color(0xFFFDFCFF)

private val Secondary10 = Color(0xFF002022)
private val Secondary20 = Color(0xFF003739)
private val Secondary30 = Color(0xFF005054)
private val Secondary40 = iOSTeal
private val Secondary50 = Color(0xFF00848C)
private val Secondary60 = Color(0xFF00A0A9)
private val Secondary70 = Color(0xFF00BDC7)
private val Secondary80 = Color(0xFF4FD8E5)
private val Secondary90 = Color(0xFF90F4FF)
private val Secondary95 = Color(0xFFE0FCFF)

private val Tertiary10 = Color(0xFF210B38)
private val Tertiary20 = Color(0xFF371A4D)
private val Tertiary30 = Color(0xFF4E2564)
private val Tertiary40 = iOSPurple
private val Tertiary50 = Color(0xFF8B4190)
private val Tertiary60 = Color(0xFFA656A8)
private val Tertiary70 = Color(0xFFC18FC0)
private val Tertiary80 = Color(0xFFE5B8FF)
private val Tertiary90 = Color(0xFFF5D9FF)
private val Tertiary95 = Color(0xFFFBEBFF)

private val Error10 = Color(0xFF410002)
private val Error20 = Color(0xFF690005)
private val Error30 = Color(0xFF93000A)
private val Error40 = iOSRed
private val Error50 = Color(0xFFDE3730)
private val Error60 = Color(0xFFFF5449)
private val Error70 = Color(0xFFFF897D)
private val Error80 = Color(0xFFFFB4AB)
private val Error90 = Color(0xFFFFDAD6)
private val Error95 = Color(0xFFFFEDEA)

private val Neutral0 = Color(0xFF000000)
private val Neutral4 = Color(0xFF0F0F11)
private val Neutral6 = Color(0xFF141316)
private val Neutral10 = Color(0xFF1C1C1E)
private val Neutral12 = Color(0xFF201F24)
private val Neutral17 = Color(0xFF2A292D)
private val Neutral20 = Color(0xFF313033)
private val Neutral22 = Color(0xFF363439)
private val Neutral24 = Color(0xFF3B383D)
private val Neutral30 = Color(0xFF484649)
private val Neutral40 = Color(0xFF605D62)
private val Neutral50 = Color(0xFF78767A)
private val Neutral60 = Color(0xFF939094)
private val Neutral70 = Color(0xFFAEAAAE)
private val Neutral80 = Color(0xFFC9C5CA)
private val Neutral87 = Color(0xFFDED8DE)
private val Neutral90 = Color(0xFFE6E1E5)
private val Neutral92 = Color(0xFFECE7EC)
private val Neutral94 = Color(0xFFF2EFF4)
private val Neutral95 = Color(0xFFF4EFF4)
private val Neutral96 = Color(0xFFF7F3F7)
private val Neutral98 = Color(0xFFFDFBFF)
private val Neutral99 = Color(0xFFFFFBFF)
private val Neutral100 = Color(0xFFFFFFFF)

private val SurfaceLight = Color(0xFFF2F2F7)
private val SurfaceDark = Color(0xFF000000)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Neutral100,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary10,
    secondary = Secondary40,
    onSecondary = Neutral100,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = Neutral100,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    error = Error40,
    onError = Neutral100,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = SurfaceLight,
    onBackground = Neutral10,
    surface = Neutral100,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral50,
    outline = Neutral30,
    outlineVariant = Neutral80,
    scrim = Neutral0,
    inverseSurface = Neutral10,
    inverseOnSurface = Neutral98,
    inversePrimary = Primary80,
    surfaceDim = Neutral87,
    surfaceBright = Neutral98,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral95,
    surfaceContainerHigh = Neutral90,
    surfaceContainerHighest = Neutral87,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary10,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    secondary = Secondary80,
    onSecondary = Secondary10,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary10,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    error = Error80,
    onError = Error10,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = SurfaceDark,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral70,
    outline = Neutral50,
    outlineVariant = Neutral30,
    scrim = Neutral0,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral10,
    inversePrimary = Primary40,
    surfaceDim = Neutral10,
    surfaceBright = Neutral20,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
)

@Composable
fun EasyPrintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}