package com.coffeepeek.admin.sandbox

import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistoryCalendar
import com.coffeepeek.admin.di.checkinhistory.HomeHistoryReaderAdapter
import com.coffeepeek.admin.di.homecheckin.androidHomeCheckInDates
import com.coffeepeek.admin.sandbox.backend.SandboxBackend
import com.coffeepeek.admin.sandbox.compose.SandboxPhoto
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.checkinhistory.data.repository.createCheckInHistoryRepository
import com.coffeepeek.feature.checkinhistory.impl.createCheckInHistoryEntry
import com.coffeepeek.feature.homecheckin.data.repository.createHomePreparationRepository
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationUpload
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInDetailEntry
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import java.util.UUID

/** Sandbox-only manual composition. No root Koin, production session, auth/client or analytics SDK. */
internal class SandboxEnvironment {
    private val original = sandboxImageBytes(false)
    private val cutout = sandboxImageBytes(true)
    val backend = SandboxBackend(original, cutout)
    val repository = createHomePreparationRepository(backend.gateway, backend.uploader, SandboxBackend.BASE_URL)
    var alive = true
        private set
    val history = createCheckInHistoryEntry(
        createCheckInHistoryRepository(backend.gateway, SandboxBackend.BASE_URL), HomeHistoryReaderAdapter(repository),
        AndroidCheckInHistoryCalendar()::snapshot,
        { url, modifier, _ -> SandboxPhoto(backend.photo(url), modifier) },
    )
    val detail = createHomeCheckInDetailEntry(repository, { alive }) { url, _, modifier, _ ->
        SandboxPhoto(backend.photo(url), modifier)
    }
    val editor = createHomeCheckInEditorEntry(repository,
        object : HomeCheckInEditorCatalog {
            override suspend fun brewMethods() = Result.success(listOf(
                HomeCheckInBrewMethod("hario-v60", "Hario V60"), HomeCheckInBrewMethod("aeropress", "AeroPress")))
            override suspend fun searchBeans(query: String) = Result.success(
                listOf(HomeCheckInBeanOption("sandbox-ethiopia", "Sandbox Ethiopia", "Sandbox Roaster"))
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) })
        },
        { Result.success(listOf(HomePreparationUpload("sandbox-cutout.png", "image/png", cutout.copyOf()))) },
        { alive }, androidHomeCheckInDates(),
        HomeCheckInEditorMedia(
            { Icon(CoffeePeekIcons.Coffee, null, it) }, { Icon(CoffeePeekIcons.Coffee, null, it) },
            { _, modifier -> Icon(CoffeePeekIcons.Coffee, null, modifier) },
            { photo, modifier, _ -> SandboxPhoto(photo.bytes, modifier) }),
        object : HomeCheckInPhotoPicker {
            @Composable override fun rememberController() = HomeCheckInPhotoPickerController(false,
                { remaining, deliver -> if (remaining > 0) deliver(listOf(photo())) },
                { deliver -> deliver(listOf(photo())) })
            private fun photo() = HomePreparationUpload("sandbox-${UUID.randomUUID()}.png", "image/png", original.copyOf())
        },
    )
    fun close() { alive = false; backend.close() }
}
