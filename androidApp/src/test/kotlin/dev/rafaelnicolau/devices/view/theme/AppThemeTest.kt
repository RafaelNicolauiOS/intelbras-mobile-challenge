package dev.rafaelnicolau.devices.view.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun paletteMatchesTheStyleGuide() {
        assertEquals(Color(0xFF00B26B), AppColors.Primary)
        assertEquals(Color(0xFF253B40), AppColors.Text)
        assertEquals(Color(0xFFD72736), AppColors.Error)
        assertEquals(Color(0xFFFFFFFF), AppColors.Background)
    }

    @Test
    fun shapesMatchTheStyleGuide() {
        assertEquals(RoundedCornerShape(8.dp), AppShapes.Card)
        assertEquals(RoundedCornerShape(4.dp), AppShapes.Field)
        assertEquals(RoundedCornerShape(percent = 50), AppShapes.Pill)
    }

    @Test
    fun themeAppliesTheBrandColorsAndTypography() {
        var primary = Color.Unspecified
        var error = Color.Unspecified
        lateinit var titleWeight: FontWeight
        var bodySize = 0f
        compose.setContent {
            AppTheme {
                primary = MaterialTheme.colorScheme.primary
                error = MaterialTheme.colorScheme.error
                titleWeight = MaterialTheme.typography.titleLarge.fontWeight!!
                bodySize = MaterialTheme.typography.bodyLarge.fontSize.value
            }
        }

        assertEquals(AppColors.Primary, primary)
        assertEquals(AppColors.Error, error)
        assertEquals(FontWeight.SemiBold, titleWeight)
        assertEquals(16.sp.value, bodySize)
    }
}
