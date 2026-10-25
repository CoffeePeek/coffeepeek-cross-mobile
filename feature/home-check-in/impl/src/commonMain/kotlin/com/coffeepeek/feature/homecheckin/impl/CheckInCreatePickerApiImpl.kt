package com.coffeepeek.feature.homecheckin.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.homecheckin.api.CheckInCreatePickerEntry
import com.coffeepeek.feature.homecheckin.impl.ui.compose.CheckInCreatePickerContent

fun createCheckInCreatePickerEntry(): CheckInCreatePickerEntry = CheckInCreatePickerApiImpl()

private class CheckInCreatePickerApiImpl : CheckInCreatePickerEntry {
    @Composable
    override fun Content(onShop: () -> Unit, onHome: () -> Unit, modifier: Modifier) {
        var homeSelected by rememberSaveable { mutableStateOf(false) }
        CheckInCreatePickerContent(homeSelected, { homeSelected = it },
            { if (homeSelected) onHome() else onShop() }, modifier)
    }
}
