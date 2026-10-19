package com.coffeepeek.feature.coffee.impl

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButtonStyle
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.domain.model.*
import com.coffeepeek.feature.coffee.impl.ui.compose.*
import com.coffeepeek.feature.coffee.impl.ui.compose.component.*
import com.coffeepeek.feature.coffee.impl.ui.compose.model.*

/** Self-targeted fake host, never the production Application, DI or HTTP stack. */
class CoffeeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Optional read-only visual fixture inside this isolated package, using real brand fonts.
        val preview = intent.getStringExtra("fixture") ?: return
        setContent { CoffeePeekTheme(darkTheme = intent.getBooleanExtra("dark", false)) {
            Surface {
                when (preview) {
                    "likes" -> Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Выбрано: Glass / Plain · Не выбрано: Glass / Plain")
                        for (background in listOf(Color.White, Color(0xFF8D6E63), Color.Black)) {
                            Row(Modifier.fillMaxWidth().height(96.dp).background(background),
                                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                for (selected in listOf(true, false)) {
                                    CoffeePeekLikeButton(selected, "Glass", {}, overImage = true)
                                    CoffeePeekLikeButton(selected, "Plain", {}, overImage = true, style = CoffeePeekLikeButtonStyle.Plain)
                                }
                            }
                        }
                    }
                    "list" -> CoffeeListScreenContent(CoffeeListState(items = listOf(previewCoffee()), isLoading = false), {},
                        { _, description, modifier, _ -> CoffeePeekImagePlaceholder(modifier, contentDescription = description) })
                    "detail" -> CoffeeDetailScreenContent(CoffeeDetailState(details = previewCoffeeDetails(), isLoading = false), {},
                        { _, description, modifier, _ -> CoffeePeekImagePlaceholder(modifier, contentDescription = description) }, { _, _, _ -> }, { it })
                    "filters" -> CoffeeFiltersSheet(CoffeeListState(filterDraft = CoffeeFilters(), filtersLoading = false,
                        filterGroups = listOf(CoffeeFilterGroup("taste", "Вкус", listOf(CoffeeFilterOption("berries", "Ягоды"))))), {})
                }
            }
        } }
    }
}
