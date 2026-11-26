package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.feature.checkinhistory.impl.OwnProfileHistoryRow
import com.coffeepeek.feature.checkinhistory.impl.OwnProfileHistorySnapshot
import com.coffeepeek.feature.profile.impl.OwnProfileSnapshot

internal fun OwnProfileSnapshot.iosHistoryState(history: OwnProfileHistorySnapshot, language: String): IosOwnProfileState {
    val state = iosState()
    return state.copy(checkInsLoading = active && history.loading, checkInsFailed = active && history.failed,
        checkIns = if (active) history.rows.map { it.iosCheckIn(language, state.identity) } else emptyList(),
        moreCheckIns = active && history.more, loadingMoreCheckIns = active && history.loadingMore,
        moreCheckInsFailed = active && history.moreFailed, selectedDate = history.selectedDate.takeIf { active })
}

private fun OwnProfileHistoryRow.iosCheckIn(language: String, identity: IosOwnProfileIdentity?): IosOwnProfileCheckIn {
    val owner = shop?.username?.takeIf(String::isNotBlank) ?: identity?.name.orEmpty()
    val scores = shop?.rating?.let { listOfNotNull(it.coffee, it.service, it.place) }.orEmpty()
    return IosOwnProfileCheckIn(key, owner, shop?.authorAvatarUrl ?: identity?.avatar,
        shop?.shopName.orEmpty(), shop?.shop?.slug, home != null, shop?.note ?: home?.note.orEmpty(),
        utcIsoToLocalDateTime(timestamp), drink(language), shop?.helpfulCount ?: 0,
        shop?.isHelpfulByCurrentUser ?: false,
        scores.takeIf { it.size == 3 }?.let { (kotlin.math.round(it.average() * 10) / 10).toString() },
        originals.indices.map { IosOwnProfilePhoto(displayPhoto(it), canOpenPhoto(it), fallbackPhoto(it)) }, brew())
}

private fun OwnProfileHistoryRow.drink(language: String): String? {
    home?.let { return it.customDrinkName?.takeIf(String::isNotBlank) ?: it.drinkName }
    val drink = shop?.drink ?: return null
    return drink.customName?.takeIf(String::isNotBlank) ?: if (language.startsWith("en")) {
        drink.nameEn?.takeIf(String::isNotBlank) ?: drink.nameRu
    } else drink.nameRu?.takeIf(String::isNotBlank) ?: drink.nameEn
}

private fun OwnProfileHistoryRow.brew(): IosOwnProfileBrew? = home?.let { row ->
    val recipe = row.recipe
    IosOwnProfileBrew(recipe.beanName, recipe.roasterName, recipe.brewMethodCode, recipe.doseGrams,
        recipe.brewDurationSeconds ?: -1, recipe.tasteScore ?: -1,
        row.shelfBagCard?.let { it.thumbnailUrl ?: it.url }, recipe.coffeeProductSlug)
}
