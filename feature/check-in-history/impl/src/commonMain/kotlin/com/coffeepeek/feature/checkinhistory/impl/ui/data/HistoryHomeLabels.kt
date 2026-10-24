package com.coffeepeek.feature.checkinhistory.impl.ui.data

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryModeration
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryVisibility
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable internal fun HomeHistoryEntry.visibilityLabel(): String? = when (visibility) {
    HistoryVisibility.Private -> stringResource(Res.string.history_private)
    HistoryVisibility.Public -> stringResource(when (moderation) {
        HistoryModeration.Approved -> Res.string.history_visible
        HistoryModeration.Pending -> Res.string.history_pending
        HistoryModeration.Rejected -> Res.string.history_rejected
        HistoryModeration.NotSubmitted -> Res.string.history_waiting_publication
        HistoryModeration.Unknown -> Res.string.history_public
    })
    null -> null
}

@Composable internal fun historyBrewMethodLabel(code: String): String = when (code) {
    "espresso" -> stringResource(Res.string.history_method_espresso)
    "pour_over" -> stringResource(Res.string.history_method_pour_over)
    "batch_drip" -> stringResource(Res.string.history_method_batch)
    "moka_pot" -> stringResource(Res.string.history_method_moka)
    "cezve" -> stringResource(Res.string.history_method_cezve)
    "aeropress" -> stringResource(Res.string.history_method_aeropress)
    "french_press" -> stringResource(Res.string.history_method_french_press)
    "other" -> stringResource(Res.string.history_method_other)
    "hario-v60" -> stringResource(Res.string.history_method_hario)
    "chemex" -> stringResource(Res.string.history_method_chemex)
    "cold-brew" -> stringResource(Res.string.history_method_cold_brew)
    "batch-brew" -> stringResource(Res.string.history_method_batch_brew)
    "moka-pot" -> stringResource(Res.string.history_method_moka_pot)
    "cezve-turkish-coffee" -> stringResource(Res.string.history_method_cezve_turkish)
    else -> code // Preserve unknown saved/catalog codes, as the current screen does.
}
