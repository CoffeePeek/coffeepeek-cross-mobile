package com.coffeepeek.feature.shop.impl

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInCreateScreenContent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState

/** Self-targeted test APK only: deterministic visual fixture, no DI, account, picker or HTTP. */
class ShopFormTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.getBooleanExtra("parity_preview", false)) return
        val dark = intent.getBooleanExtra("parity_dark", true)
        enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent {
            var state by remember { mutableStateOf(ShopCheckInFormState(
                ShopCheckInCreateInput("fixture", "", ShopRating(4, 4, 4), "2026-10-14T10:00:00Z"), isLoading = false)) }
            CoffeePeekTheme(darkTheme = dark) {
                CoffeePeekSwipeDismissModalBottomSheet({ finish() }, "Закрыть форму", modifier = Modifier.fillMaxHeight(0.76f)) {
                    ShopCheckInCreateScreenContent(state, "Memories", "14 октября 2026", 1791936000000, 1791972000000,
                        onAction = { action ->
                            state = when (action) {
                                is ShopCheckInFormAction.TextChanged -> state.copy(input = state.input.copy(text = action.value.take(1000)))
                                is ShopCheckInFormAction.RatingChanged -> state.copy(input = state.input.copy(rating = action.rating))
                                is ShopCheckInFormAction.VisibilityChanged -> state.copy(input = state.input.copy(visibility = action.visibility))
                                else -> state // In particular: fixture Submit never writes anything.
                            }
                        }, onVisitDate = {}, onGallery = {}, onCamera = {}, modifier = Modifier.fillMaxHeight())
                }
            }
        }
    }
}
