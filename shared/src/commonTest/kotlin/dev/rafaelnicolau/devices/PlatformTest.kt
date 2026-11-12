package dev.rafaelnicolau.devices

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun greetingMentionsThePlatform() {
        assertTrue(greeting().startsWith("Shared module running on "))
    }
}
