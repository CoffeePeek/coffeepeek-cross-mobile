package com.coffeepeek.feature.coffee.impl.ui.data

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.coffee.impl.resources.Res
import com.coffeepeek.feature.coffee.impl.resources.coffee_capsules
import com.coffeepeek.feature.coffee.impl.resources.coffee_drip_bags
import com.coffeepeek.feature.coffee.impl.resources.coffee_green
import com.coffeepeek.feature.coffee.impl.resources.coffee_ground
import com.coffeepeek.feature.coffee.impl.resources.coffee_roasted
import com.coffeepeek.feature.coffee.impl.resources.coffee_whole_beans
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun coffeeProductFormLabel(code: String?): String? = when (code) {
    "whole_beans" -> stringResource(Res.string.coffee_whole_beans)
    "ground" -> stringResource(Res.string.coffee_ground)
    "capsules" -> stringResource(Res.string.coffee_capsules)
    "drip_bags" -> stringResource(Res.string.coffee_drip_bags)
    else -> null
}

@Composable
internal fun coffeeProductKindLabel(code: String?): String? = when (code) {
    "roasted_beans" -> stringResource(Res.string.coffee_roasted)
    "green_beans" -> stringResource(Res.string.coffee_green)
    else -> null
}
