package com.coffeepeek.core.designsystem

import android.graphics.Typeface
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.Manrope
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.security.MessageDigest

@OptIn(ExperimentalResourceApi::class)
class BrandFontContractTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun render(content: @Composable () -> Unit): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent { content() } }
        }

    @Test fun packagedFontsAreExactLegacyCopiesWithLicense() = runBlocking {
        val hashes = mapOf(
            "light" to "af0f3214a2e3a0eb55ca7c699c751692ef6633c7edfb4c1669bdaa7715d5d129",
            "regular" to "d9ad227c0d7202062be2d07684edf89b67d4bb50234fafe3abf23be0df390d1b",
            "medium" to "6926e8266ac5f9c64870ae9c39734c0d2eb616cdd2f67e5253728cc9873d0984",
            "semibold" to "20f024ed66b72f059d394d8f91c5e220a0391d7ba78e511c441b8dfda02fd77e",
            "bold" to "3e82951bcb91009ca4b7e829879202d631afda71fe2b5b5c1553bf7bd55fcefa",
            "extrabold" to "06f202ed8c4f3ac06e8d636dc3296b5662698610a1367e50205f46cb7c911b6c",
        )
        hashes.forEach { (weight, expected) ->
            val bytes = Res.readBytes("font/manrope_$weight.ttf")
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals(weight, expected, actual)
        }
        val license = Res.readBytes("files/licenses/manrope-OFL.txt").decodeToString()
        assertTrue(license.contains("Copyright 2019 The Manrope Project Authors"))
        assertTrue(license.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(license.contains("PERMISSION & CONDITIONS"))
    }

    @Test fun allSixWeightsResolveAndRenderCyrillic() {
        val weights = listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium,
            FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
        val resolved = mutableMapOf<Int, Typeface>()
        render {
            CoffeePeekTheme {
                val family = Manrope
                val resolver = LocalFontFamilyResolver.current
                Column {
                    weights.forEach { weight ->
                        resolved[weight.weight] = resolver.resolve(family, weight).value as Typeface
                        Text("Кофе Coffee ${weight.weight}", fontWeight = weight)
                    }
                }
            }
        }.use {
            weights.forEach { compose.onNodeWithText("Кофе Coffee ${it.weight}").assertIsDisplayed() }
            compose.runOnIdle {
                assertEquals(6, resolved.size)
                if (Build.VERSION.SDK_INT >= 28) {
                    resolved.forEach { (weight, typeface) -> assertEquals(weight, typeface.weight) }
                }
            }
        }
    }

    @Test fun defaultThemeUsesBrandFamilyInBothModes() {
        listOf(false, true).forEach { dark ->
            var family: FontFamily? = null
            var brand: FontFamily? = null
            render {
                CoffeePeekTheme(darkTheme = dark) {
                    brand = Manrope
                    family = MaterialTheme.typography.bodyLarge.fontFamily
                    Text(if (dark) "Dark typography" else "Light typography")
                }
            }.use {
                compose.onNodeWithText(if (dark) "Dark typography" else "Light typography").assertIsDisplayed()
                compose.runOnIdle { assertNotNull(brand); assertEquals(brand, family) }
            }
        }
    }

    @Test fun callerFontOverrideStillWorks() {
        var family: FontFamily? = null
        render {
            CoffeePeekTheme(FontFamily.Monospace) {
                family = MaterialTheme.typography.bodyLarge.fontFamily
                Text("Override")
            }
        }.use {
            compose.onNodeWithText("Override").assertIsDisplayed()
            compose.runOnIdle { assertEquals(FontFamily.Monospace, family) }
        }
    }
}
