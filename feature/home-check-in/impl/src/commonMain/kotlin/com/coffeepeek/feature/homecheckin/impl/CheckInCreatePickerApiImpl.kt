package com.coffeepeek.feature.homecheckin.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.homecheckin.api.CheckInCreatePickerEntry
import com.coffeepeek.feature.homecheckin.impl.ui.compose.CheckInCreatePickerContent

fun createCheckInCreatePickerEntry(): CheckInCreatePickerEntry = CheckInCreatePickerApiImpl()

private class CheckInCreatePickerApiImpl : CheckInCreatePickerEntry {
    @Composable
    override fun Content(onShop: () -> Unit, onHome: () -> Unit, modifier: Modifier, onShelfBag: (() -> Unit)?) {
        CheckInCreatePickerContent(onShop, onHome, onShelfBag, modifier)
    }
}
