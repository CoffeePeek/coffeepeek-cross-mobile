package com.coffeepeek.core.designsystem.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.coffeepeek.core.designsystem.resources.Res
import com.coffeepeek.core.designsystem.resources.map_coffeepeek_bean
import com.coffeepeek.core.designsystem.resources.map_cup_fill
import com.coffeepeek.core.designsystem.resources.map_fork_knife
import org.jetbrains.compose.resources.painterResource

/** The original map marker glyphs, shared with venue filter chips. */
enum class CoffeePeekMapIcon { Specialty, CoffeeShop, Cafe }

@Composable
fun coffeePeekMapIconPainter(icon: CoffeePeekMapIcon): Painter = painterResource(when (icon) {
    CoffeePeekMapIcon.Specialty -> Res.drawable.map_coffeepeek_bean
    CoffeePeekMapIcon.CoffeeShop -> Res.drawable.map_cup_fill
    CoffeePeekMapIcon.Cafe -> Res.drawable.map_fork_knife
})
