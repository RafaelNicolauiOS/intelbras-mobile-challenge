package dev.rafaelnicolau.devices

import androidx.test.core.app.ApplicationProvider
import dev.rafaelnicolau.devices.di.AppContainer
import dev.rafaelnicolau.devices.repository.DefaultDeviceRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DevicesApplicationTest {
    @Test
    fun startupWiresTheRealRepository() {
        // Robolectric creates the Application declared in the manifest before each test.
        ApplicationProvider.getApplicationContext<DevicesApplication>()

        assertTrue(AppContainer.deviceRepository is DefaultDeviceRepository)
    }
}
