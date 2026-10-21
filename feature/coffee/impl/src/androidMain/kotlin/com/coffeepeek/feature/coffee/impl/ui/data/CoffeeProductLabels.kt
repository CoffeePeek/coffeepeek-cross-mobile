package com.coffeepeek.feature.coffee.impl.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.coffeepeek.feature.coffee.impl.R

@Composable
internal fun coffeeProductFormLabel(code: String?): String? = when (code) {
    "whole_beans" -> stringResource(R.string.coffee_whole_beans)
    "ground" -> stringResource(R.string.coffee_ground)
    "capsules" -> stringResource(R.string.coffee_capsules)
    "drip_bags" -> stringResource(R.string.coffee_drip_bags)
    else -> null
}

@Composable
internal fun coffeeProductKindLabel(code: String?): String? = when (code) {
    "roasted_beans" -> stringResource(R.string.coffee_roasted)
    "green_beans" -> stringResource(R.string.coffee_green)
    else -> null
}
