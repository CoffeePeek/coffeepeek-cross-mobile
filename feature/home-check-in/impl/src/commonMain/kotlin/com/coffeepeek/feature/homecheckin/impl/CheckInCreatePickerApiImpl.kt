package com.coffeepeek.feature.homecheckin.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.homecheckin.api.CheckInCreatePickerEntry
import com.coffeepeek.feature.homecheckin.impl.ui.compose.CheckInCreateChoice
import com.coffeepeek.feature.homecheckin.impl.ui.compose.CheckInCreatePickerContent

fun createCheckInCreatePickerEntry(): CheckInCreatePickerEntry = CheckInCreatePickerApiImpl()

private class CheckInCreatePickerApiImpl : CheckInCreatePickerEntry {
    @Composable
    override fun Content(onShop: () -> Unit, onHome: () -> Unit, modifier: Modifier, onShelfBag: (() -> Unit)?) {
        var choice by rememberSaveable { mutableStateOf(CheckInCreateChoice.Shop) }
        CheckInCreatePickerContent(choice, { choice = it }, {
            when (choice) {
                CheckInCreateChoice.Shop -> onShop()
                CheckInCreateChoice.Home -> onHome()
                CheckInCreateChoice.ShelfBag -> onShelfBag?.invoke() ?: onShop()
            }
        }, modifier, shelfAvailable = onShelfBag != null)
    }
}
