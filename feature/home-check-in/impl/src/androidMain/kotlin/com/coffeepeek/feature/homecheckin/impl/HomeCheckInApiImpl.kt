package com.coffeepeek.feature.homecheckin.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.homecheckin.api.HomeCheckInDetailEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.ui.HomeCheckInDetailViewModel
import com.coffeepeek.feature.homecheckin.impl.ui.compose.HomeCheckInDetailScreen
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.HomeCheckInDetailEvent

/**
 * [repository] and [canAccess] must be bound to the same account lease. Construct
 * once per lease, not on each recomposition. Repository guards remain mandatory.
 * [imageContent] uses the configured authenticated loader; cutouts must preserve
 * transparency, fit inside their bounds and keep the existing 3dp outline.
 * Originals crop to their bounds. No client, picker or root navigation is owned here.
 */
fun createHomeCheckInDetailEntry(
    repository: HomePreparationRepository,
    canAccess: () -> Boolean,
    imageContent: @Composable (url: String, description: String, modifier: Modifier, isCutout: Boolean) -> Unit,
): HomeCheckInDetailEntry = HomeCheckInApiImpl(repository, canAccess, imageContent)

private class HomeCheckInApiImpl(
    private val repository: HomePreparationRepository,
    private val canAccess: () -> Boolean,
    private val imageContent: @Composable (String, String, Modifier, Boolean) -> Unit,
) : HomeCheckInDetailEntry {
    @Composable
    override fun Content(
        preparationId: String,
        sessionGeneration: Int,
        accessAllowed: Boolean,
        formatPreparedAt: (String, String) -> String,
        formatBrewMethod: (String) -> String,
        onClose: () -> Unit,
        onEdit: (String) -> Unit,
        onDeleted: (String) -> Unit,
        onOpenPhoto: (List<String>, Int) -> Unit,
        onDismissAllowedChange: (Boolean) -> Unit,
        modifier: Modifier,
        notice: String?,
    ) {
        // Account replacement must not reuse a VM or its private state/effect queue.
        key(this, sessionGeneration, preparationId) {
            val currentAccess by rememberUpdatedState(accessAllowed)
            val model = viewModel(key = "home-detail:$sessionGeneration:$preparationId") {
                HomeCheckInDetailViewModel(preparationId, repository, sessionGeneration) { currentAccess && canAccess() }
            }
            HomeCheckInDetailScreen(model, accessAllowed && canAccess(), formatPreparedAt,
                formatBrewMethod, imageContent, modifier, notice, onDismissAllowedChange) { event ->
                when (event) {
                    HomeCheckInDetailEvent.Close -> onClose()
                    is HomeCheckInDetailEvent.Edit -> onEdit(event.preparationId)
                    is HomeCheckInDetailEvent.Deleted -> onDeleted(event.preparationId)
                    is HomeCheckInDetailEvent.OpenPhoto -> onOpenPhoto(event.urls, event.index)
                }
            }
        }
    }
}
