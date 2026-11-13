package dev.rafaelnicolau.devices.coordinator

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Navigation decisions, checked on a real NavHost with empty destinations. */
@RunWith(RobolectricTestRunner::class)
class DevicesCoordinatorTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var navController: TestNavHostController
    private lateinit var coordinator: DevicesCoordinator

    @Before
    fun setUp() {
        compose.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            coordinator = DevicesCoordinator(navController)
            NavHost(navController, startDestination = Route.DASHBOARD) {
                composable(Route.DASHBOARD) {}
                composable(
                    Route.DEVICE_DETAILS,
                    arguments = listOf(navArgument(Route.ARG_DEVICE_ID) { type = NavType.StringType }),
                ) {}
                composable(
                    Route.DEVICE_FORM,
                    arguments = listOf(
                        navArgument(Route.ARG_DEVICE_ID) {
                            type = NavType.StringType
                            nullable = true
                        },
                    ),
                ) {}
            }
        }
    }

    private fun currentRoute() = navController.currentBackStackEntry?.destination?.route

    private fun currentDeviceId() = navController.currentBackStackEntry?.arguments?.getString(Route.ARG_DEVICE_ID)

    @Test
    fun startsOnTheDashboard() {
        assertEquals(Route.DASHBOARD, currentRoute())
    }

    @Test
    fun showDeviceDetailsOpensTheDetailsOfThatDevice() {
        compose.runOnUiThread { coordinator.showDeviceDetails("v1") }

        assertEquals(Route.DEVICE_DETAILS, currentRoute())
        assertEquals("v1", currentDeviceId())
    }

    @Test
    fun showAddDeviceOpensAnEmptyForm() {
        compose.runOnUiThread { coordinator.showAddDevice() }

        assertEquals(Route.DEVICE_FORM, currentRoute())
        assertNull(currentDeviceId())
    }

    @Test
    fun showEditDeviceOpensTheFormForThatDevice() {
        compose.runOnUiThread { coordinator.showEditDevice("a1") }

        assertEquals(Route.DEVICE_FORM, currentRoute())
        assertEquals("a1", currentDeviceId())
    }

    @Test
    fun backReturnsToThePreviousScreen() {
        compose.runOnUiThread {
            coordinator.showDeviceDetails("v1")
            coordinator.back()
        }

        assertEquals(Route.DASHBOARD, currentRoute())
    }
}
