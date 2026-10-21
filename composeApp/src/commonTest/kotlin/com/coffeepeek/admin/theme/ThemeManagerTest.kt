package com.coffeepeek.admin.theme

import com.coffeepeek.admin.settings.FakeSettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Parity tests for the legacy global `ThemeManager` (settings migration step 1). It is a process-wide `object` whose `initialize`
 * runs once, so the persistence rules live in one ordered scenario; the manager uses real dispatchers, so assertions wait for their condition.
 */
class ThemeManagerTest {
    private suspend fun await(condition: () -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!condition()) delay(10) } }

    @Test fun theSavedModeIsRestoredAnInvalidOrMissingValueMeansSystemAndAChoiceIsPublishedBeforeItIsSaved() = runTest {
        val settings = FakeSettingRepository(mapOf("theme_mode" to "DARK"))
        ThemeManager.initialize(settings)
        await { ThemeManager.themeMode.value == ThemeMode.DARK }

        settings.put("theme_mode", "NEON")
        await { ThemeManager.themeMode.value == ThemeMode.SYSTEM }
        settings.put("theme_mode", "LIGHT")
        await { ThemeManager.themeMode.value == ThemeMode.LIGHT }
        settings.delete("theme_mode")
        await { ThemeManager.themeMode.value == ThemeMode.SYSTEM }

        // The flow changes synchronously; the setting follows asynchronously under the same key and the enum name.
        ThemeManager.setTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemeManager.themeMode.value)
        await { settings.valueOf("theme_mode") == "DARK" }
        assertEquals("theme_mode", settings.saves.last().key)

        // `initialize` is idempotent: a second repository is ignored, saves keep going to the first one.
        val other = FakeSettingRepository()
        ThemeManager.initialize(other)
        ThemeManager.setTheme(ThemeMode.LIGHT)
        await { settings.valueOf("theme_mode") == "LIGHT" }
        delay(100)
        assertEquals(emptyList(), other.saves)
        ThemeManager.setTheme(ThemeMode.SYSTEM)
        await { settings.valueOf("theme_mode") == "SYSTEM" }
    }

    @Test fun allThreeModesExistInTheDocumentedOrder() {
        assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK), ThemeMode.entries)
    }
}
