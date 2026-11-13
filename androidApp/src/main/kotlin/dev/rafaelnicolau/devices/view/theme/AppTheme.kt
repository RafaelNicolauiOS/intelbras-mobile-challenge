package dev.rafaelnicolau.devices.view.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rafaelnicolau.devices.R

/** The palette from the style guide, with the names it uses there. */
object AppColors {
    val Primary = Color(0xFF00B26B)
    val PrimarySoft = Color(0xFFD6F2E7)
    val Text = Color(0xFF253B40)
    val TextSecondary = Color(0xFF9DB3BD)
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFAFAFA)
    val SurfaceStrong = Color(0xFFF1F1F1)
    val BorderMedium = Color(0xFFDEDFE0)
    val BorderButton = Color(0xFFBEC2C4)
    val Error = Color(0xFFD72736)
    val SurfaceDark = Color(0xFF1A1B1D)
    val TextOnDark = Color(0xFFF5EFF7)
}

object AppShapes {
    val Card = RoundedCornerShape(8.dp)
    val Field = RoundedCornerShape(4.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

private val colorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    secondaryContainer = AppColors.PrimarySoft,
    onSecondaryContainer = AppColors.Primary,
    background = AppColors.Background,
    onBackground = AppColors.Text,
    surface = AppColors.Background,
    onSurface = AppColors.Text,
    onSurfaceVariant = AppColors.TextSecondary,
    surfaceContainerHigh = AppColors.Surface,
    surfaceContainerLow = AppColors.Background,
    outline = AppColors.TextSecondary,
    outlineVariant = AppColors.BorderMedium,
    error = AppColors.Error,
    onError = Color.White,
    inverseSurface = AppColors.SurfaceDark,
    inverseOnSurface = AppColors.TextOnDark,
)

// The style guide uses a single family, Open Sans, in two weights: Regular (400) and SemiBold (600).
// One variable font file provides both.
@OptIn(ExperimentalTextApi::class)
private val OpenSans = FontFamily(
    Font(R.font.open_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.open_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = OpenSans, fontSize = size.sp, lineHeight = lineHeight.sp, fontWeight = weight)

// Every slot is set, so Material components that pick a default style also use Open Sans.
private val typography = Typography(
    displayLarge = style(57, 64),
    displayMedium = style(45, 52),
    displaySmall = style(36, 44),
    headlineLarge = style(32, 40, FontWeight.SemiBold),
    headlineMedium = style(24, 32, FontWeight.SemiBold),
    headlineSmall = style(22, 28, FontWeight.SemiBold),
    titleLarge = style(20, 28, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24),
    bodyMedium = style(14, 20),
    bodySmall = style(12, 16),
    labelLarge = style(14, 20, FontWeight.SemiBold),
    labelMedium = style(12, 16, FontWeight.SemiBold),
    labelSmall = style(11, 16, FontWeight.SemiBold),
)

/** The prototype defines a single, light theme, so the app does not follow the system dark mode. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
