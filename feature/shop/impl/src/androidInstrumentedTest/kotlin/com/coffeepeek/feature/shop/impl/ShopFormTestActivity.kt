package com.coffeepeek.feature.shop.impl

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.model.ShopRating
import com.coffeepeek.feature.shop.domain.model.MenuPhoto
import com.coffeepeek.feature.shop.domain.model.ScheduleInterval
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopDetails
import com.coffeepeek.feature.shop.domain.model.ShopMenu
import com.coffeepeek.feature.shop.domain.model.ShopOverview
import com.coffeepeek.feature.shop.domain.model.ShopSchedule
import com.coffeepeek.feature.shop.domain.model.ShopConsumedDrinkOption
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopPhotoSourceSheet
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInDrinkField
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopCheckInCard
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopScheduleSection
import com.coffeepeek.feature.shop.impl.ui.compose.component.previewCheckIn
import com.coffeepeek.feature.shop.impl.ui.compose.ShopDetailScreenContent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopDetailState
import com.coffeepeek.feature.shop.impl.ui.compose.ShopCheckInCreateScreenContent
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopCheckInFormState

/** Self-targeted test APK only: deterministic visual fixture, no DI, account, picker or HTTP. */
class ShopFormTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val detailPreview = intent.getBooleanExtra("detail_parity_preview", false)
        val photoSourcePreview = intent.getBooleanExtra("photo_source_preview", false)
        val drinkPreview = intent.getBooleanExtra("drink_controls_preview", false)
        val footerPreview = intent.getBooleanExtra("check_in_footer_preview", false)
        if (!detailPreview && !photoSourcePreview && !drinkPreview && !footerPreview && !intent.getBooleanExtra("parity_preview", false)) return
        val dark = intent.getBooleanExtra("parity_dark", true)
        enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        if (footerPreview) {
            setContent {
                CoffeePeekTheme(darkTheme = dark) {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 48.dp)) {
                            var expanded by remember { mutableStateOf(false) }
                            ShopScheduleSection(shopDetailParityState().details!!.schedules, 4, expanded,
                                { expanded = !expanded }, isOpen = true, minuteOfDay = 720)
                            ShopCheckInCard(previewCheckIn().copy(drinkNameRu = "Капучино"), { _, _ -> },
                                modifier = Modifier.padding(top = 24.dp), showAuthor = true, showHelpful = true, onVote = {})
                        }
                    }
                }
            }
            return
        }
        if (photoSourcePreview || drinkPreview) {
            setContent {
                CoffeePeekTheme(darkTheme = dark) {
                    if (photoSourcePreview) ShopPhotoSourceSheet({ finish() }, { finish() }, { finish() })
                    else Surface {
                        Column(Modifier.padding(horizontal = 24.dp, vertical = 80.dp)) {
                            var slug by remember { mutableStateOf<String?>(null) }
                            var custom by remember { mutableStateOf("") }
                            ShopCheckInDrinkField(listOf(ShopConsumedDrinkOption("espresso", "Эспрессо", "Эспрессо"),
                                ShopConsumedDrinkOption("cappuccino", "Капучино", "Капучино"),
                                ShopConsumedDrinkOption("other", "Другое", "Другое")),
                                slug, custom, false, false, true, false, {}, { slug = it }, { custom = it })
                        }
                    }
                }
            }
            return
        }
        if (detailPreview) {
            setContent {
                CoffeePeekTheme(darkTheme = dark) {
                    ShopDetailScreenContent(shopDetailParityState(), onAction = {}, distanceLabel = "248 м")
                }
            }
            return
        }
        setContent {
            var state by remember { mutableStateOf(ShopCheckInFormState(
                ShopCheckInCreateInput("fixture", "", ShopRating(4, 4, 4), "2026-10-14T10:00:00Z"), isLoading = false)) }
            CoffeePeekTheme(darkTheme = dark) {
                CoffeePeekSwipeDismissModalBottomSheet({ finish() }, "Закрыть форму", maxSheetHeightFraction = 0.76f) {
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

/** Exact supplied screenshot shape, with intentionally empty image URLs and no live callbacks. */
internal fun shopDetailParityState() = ShopDetailState(
    details = ShopDetails(
        overview = ShopOverview("fixture", "Happy Coffee", null, "просп. Машерова 12",
            53.9, 27.5, null, 0, true, emptyList(), priceRange = "$$"),
        menu = ShopMenu(photos = listOf(MenuPhoto("one", "", ""), MenuPhoto("two", "", ""))),
        schedules = listOf(ShopSchedule(4, false, listOf(ScheduleInterval("09:00", "22:00")))),
        coffee = ShopCoffeeDetails(), contact = null, features = emptyList(), reviews = emptyList(),
        userCheckIns = emptyList(),
    ),
    isLoading = false, todayDayOfWeek = 4, todayMinuteOfDay = 12 * 60,
)
