package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.feature.homecheckin.api.*
import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.component.visibilityLabelResource
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*

@Composable
internal fun nativeHomeDetailPresentation(state: HomeCheckInDetailState, formatDate: (String, String) -> String,
    formatMethod: (String) -> String, notice: String?): HomeCheckInDetailPresentation {
    val active = !state.isClosed && !state.isAccessRevoked
    return HomeCheckInDetailPresentation(
        card = state.entry?.takeIf { active }?.let { nativeHomeDetailCard(it, formatDate, formatMethod) },
        title = stringResource(Res.string.home_check_in_home), backLabel = stringResource(Res.string.home_check_in_close),
        active = active, dismissAllowed = !state.isDeleting,
        loadingLabel = if (state.isLoading || state.isDeleting) stringResource(
            if (state.isDeleting) Res.string.home_check_in_deleting else Res.string.home_check_in_loading) else null,
        errorText = (state.deleteIssue ?: state.loadIssue)?.let { homeDetailIssueText(it, state.deleteIssue != null) },
        retryLabel = if (canRetryHomeDetail(state)) stringResource(Res.string.home_check_in_retry) else null,
        notice = notice.takeIf { active }, editLabel = stringResource(Res.string.home_check_in_edit),
        deleteLabel = stringResource(Res.string.home_check_in_delete), confirmation = homeDetailConfirmation(state),
        menuLabel = stringResource(Res.string.home_check_in_actions),
    )
}

@Composable
private fun nativeHomeDetailCard(entry: HomePreparation, date: (String, String) -> String,
    method: (String) -> String): HomeCheckInDetailCard {
    return HomeCheckInDetailCard(entry.id, stringResource(Res.string.home_check_in_home),
        date(entry.preparedOn, entry.preparedAtUtc), entry.text.orEmpty(),
        entry.beanName?.takeIf(String::isNotBlank) ?: stringResource(Res.string.home_check_in_title_fallback),
        entry.roasterName, entry.shelfBagPhoto?.let { it.thumbnailUrl ?: it.url }, nativeHomeRecipe(entry, method),
        entry.customDrinkName?.takeIf(String::isNotBlank) ?: entry.drinkName,
        entry.visibilityLabelResource()?.let { stringResource(it) },
        if (entry.moderation == HomePreparationModeration.Rejected) entry.rejectionReason?.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.home_check_in_rejection_fallback) else null,
        entry.photos.mapIndexed { index, photo -> nativeHomeDetailPhoto(photo,
            stringResource(Res.string.home_check_in_photo_description, index + 1, entry.photos.size)) },
        entry.tasteScore, entry.tasteScore?.let { stringResource(Res.string.home_check_in_rating_description, it) }.orEmpty(),
        if (entry.moderation == HomePreparationModeration.Rejected) stringResource(Res.string.home_check_in_rejected) else null)
}

@Composable
private fun nativeHomeRecipe(entry: HomePreparation, method: (String) -> String): String {
    val label = entry.brewMethodCode?.let(method)
    val methodLabel = if (entry.brewMethodCode == "other" && (label == null || label == entry.brewMethodCode))
        stringResource(Res.string.home_check_in_method_other) else label ?: stringResource(Res.string.home_check_in_unspecified)
    return listOfNotNull(methodLabel, entry.doseGrams?.let {
        val number = (if ('.' in it) it.trimEnd('0').trimEnd('.') else it).replace('.', ',')
        stringResource(Res.string.home_check_in_grams, number)
    }, entry.brewDurationSeconds?.let { stringResource(Res.string.home_check_in_seconds, it) }).joinToString(" · ")
}

@Composable
private fun homeDetailConfirmation(state: HomeCheckInDetailState): HomeCheckInDetailConfirmation? {
    if (!state.showDeleteConfirmation || state.entry == null || state.isDeleting || state.isAccessRevoked) return null
    return HomeCheckInDetailConfirmation(stringResource(Res.string.home_check_in_delete_question),
        stringResource(Res.string.home_check_in_delete_warning), stringResource(Res.string.home_check_in_delete_confirm),
        stringResource(Res.string.home_check_in_cancel))
}

@Composable
internal fun homeDetailIssueText(issue: HomePreparationIssue, deleteFailed: Boolean): String = stringResource(when (issue) {
    HomePreparationIssue.Offline -> Res.string.home_check_in_offline
    HomePreparationIssue.Timeout -> Res.string.home_check_in_timeout
    HomePreparationIssue.Unauthorized -> Res.string.home_check_in_unauthorized
    HomePreparationIssue.Forbidden -> Res.string.home_check_in_forbidden
    HomePreparationIssue.NotFound -> Res.string.home_check_in_not_found
    HomePreparationIssue.RateLimited -> Res.string.home_check_in_rate_limited
    HomePreparationIssue.Unavailable -> Res.string.home_check_in_unavailable
    else -> if (deleteFailed) Res.string.home_check_in_delete_failed else Res.string.home_check_in_load_failed
})

internal fun canRetryHomeDetail(state: HomeCheckInDetailState): Boolean = !state.isLoading && !state.isDeleting &&
    (state.deleteIssue ?: state.loadIssue)?.let { it !in setOf(HomePreparationIssue.Unauthorized,
        HomePreparationIssue.Forbidden, HomePreparationIssue.NotFound) } == true

internal fun nativeHomeDetailActions(state: HomeCheckInDetailState, send: (HomeCheckInDetailAction) -> Unit,
    onRevokedClose: () -> Unit = { send(HomeCheckInDetailAction.Close) }): HomeCheckInDetailCallbacks {
    val enabled = state.entry != null && !state.isLoading && !state.isDeleting && !state.showDeleteConfirmation && !state.isAccessRevoked
    val actions = buildMap<String, () -> Unit> {
        if (!state.isDeleting) put("back") {
            if (state.isAccessRevoked) onRevokedClose()
            else send(if (state.showDeleteConfirmation) HomeCheckInDetailAction.CancelDelete else HomeCheckInDetailAction.Close)
        }
        if (canRetryHomeDetail(state)) put("retry") { send(HomeCheckInDetailAction.Reload) }
        if (enabled) {
            put("edit") { send(HomeCheckInDetailAction.Edit) }
            put("delete") { send(HomeCheckInDetailAction.RequestDelete) }
        }
        if (state.showDeleteConfirmation && !state.isDeleting && !state.isAccessRevoked) {
            put("confirmDelete") { send(HomeCheckInDetailAction.ConfirmDelete) }
            put("cancelDelete") { send(HomeCheckInDetailAction.CancelDelete) }
        }
    }
    return HomeCheckInDetailCallbacks(actions) { if (enabled) send(HomeCheckInDetailAction.OpenPhoto(it)) }
}

internal fun nativeHomeDetailPhoto(photo: HomePreparationPhoto, label: String): HomeCheckInDetailPhoto {
    val preview = photo.thumbnailUrl ?: photo.url
    return HomeCheckInDetailPhoto(preview, photo.detailUrl?.takeIf { preview.isNotBlank() && it.isNotBlank() }, label)
}
