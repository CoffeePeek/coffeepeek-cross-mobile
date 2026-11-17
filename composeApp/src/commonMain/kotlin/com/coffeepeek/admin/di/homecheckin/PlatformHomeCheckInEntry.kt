package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInSession
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.impl.ui.data.*

internal fun platformHomeCheckInEntry(repository: HomePreparationRepository, catalog: HomeCheckInEditorCatalog,
    canAccess: () -> Boolean, dates: HomeCheckInEditorDates, media: HomeCheckInEditorMedia,
    picker: HomeCheckInPhotoPicker, analytics: (Boolean) -> HomeCheckInEditorAnalytics): HomeCheckInEditorEntry {
    val fallback = createHomeCheckInEditorEntry(repository, catalog, canAccess, dates, media, picker, analytics)
    return PlatformHomeCheckInEntry(fallback, picker) { id, generation, bean ->
        NativeHomeCheckInSession(repository, catalog, dates, canAccess, id, generation, bean, analytics(id != null))
    }
}

private class PlatformHomeCheckInEntry(
    val fallback: HomeCheckInEditorEntry,
    val picker: HomeCheckInPhotoPicker,
    val create: (String?, Int, HomeCheckInBean?) -> NativeHomeCheckInSession,
) : HomeCheckInEditorEntry {
    @Composable
    override fun Content(preparationId: String?, sessionGeneration: Int, accessAllowed: Boolean,
        onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowedChange: (Boolean) -> Unit,
        modifier: Modifier, bean: HomeCheckInBean?) {
        PlatformHomeCheckInEditor(this, preparationId, sessionGeneration, accessAllowed, picker,
            { create(preparationId, sessionGeneration, bean) }, onClose, onSaved, onDismissAllowedChange, modifier) {
            fallback.Content(preparationId, sessionGeneration, accessAllowed, onClose, onSaved,
                onDismissAllowedChange, modifier, bean)
        }
    }
}

@Composable
internal expect fun PlatformHomeCheckInEditor(owner: Any, preparationId: String?, generation: Int,
    allowed: Boolean, picker: HomeCheckInPhotoPicker, create: () -> NativeHomeCheckInSession,
    onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowed: (Boolean) -> Unit,
    modifier: Modifier, fallback: @Composable () -> Unit)
