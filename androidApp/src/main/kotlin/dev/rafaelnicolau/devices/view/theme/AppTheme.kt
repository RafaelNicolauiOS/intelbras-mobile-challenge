package dev.rafaelnicolau.devices.view.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

// The style guide uses two weights only: Regular (400) and SemiBold (600).
private val typography = Typography(
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
)

/** The prototype defines a single, light theme, so the app does not follow the system dark mode. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
