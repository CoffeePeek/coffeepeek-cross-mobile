package com.coffeepeek.admin.ui.favorites

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import com.coffeepeek.admin.MainActivity
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.favorites.api.FavoritesEntry
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesDestinationTest {
    @get:Rule val composeRule = createEmptyComposeRule()

    @Test fun openingShopUsesExistingRootRoute() {
        render().use {
            assertEvent(Navigator.NavEvent.NavigateTo(Navigator.Screen.ShopDetail("shop-42"))) {
                composeRule.onNodeWithText("Open shop").performClick()
            }
        }
    }

    @Test fun backUsesExistingRootBackStack() {
        render().use {
            assertEvent(Navigator.NavEvent.PopBack) {
                composeRule.onNodeWithText("Back").performClick()
            }
        }
    }

    private fun render() = ActivityScenario.launch(MainActivity::class.java).also { scenario ->
        scenario.onActivity { activity -> activity.setContent { FavoritesDestination(FakeEntry()) } }
    }

    private fun assertEvent(expected: Navigator.NavEvent, action: () -> Unit) = runBlocking {
        val event = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) { Navigator.navigationEvents.first() }
        }
        action()
        assertEquals(expected, event.await())
    }

    private class FakeEntry : FavoritesEntry {
        @Composable
        override fun Content(
            onOpenShop: (String) -> Unit,
            onBack: () -> Unit,
            distanceForCoordinates: (Double, Double) -> String?,
        ) {
            Column {
                Button(onClick = { onOpenShop("shop-42") }) { Text("Open shop") }
                Button(onClick = onBack) { Text("Back") }
            }
        }
    }
}
