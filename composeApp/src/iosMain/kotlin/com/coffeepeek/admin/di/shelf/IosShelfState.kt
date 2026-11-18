package com.coffeepeek.admin.di.shelf

import com.coffeepeek.feature.shelf.impl.*

data class IosShelfItem(val id: String, val title: String?, val roaster: String?, val grams: Int?, val grind: String?,
    val purpose: String?, val roastedOn: String?, val addedOn: String?, val finishedOn: String?, val days: Int?,
    val roastDays: Int?, val rating: Int?, val buyAgain: Boolean?, val hasPhoto: Boolean, val hasCoffee: Boolean)

data class IosShelfBrew(val id: String, val date: String?, val method: String?, val dose: Double?, val score: Int?)
data class IosShelfCoffee(val slug: String, val title: String, val roaster: String)
data class IosShelfPoint(val x: Float, val y: Float)
data class IosShelfStage(val name: String, val status: String, val reasons: List<String>)

data class IosShelfState(
    val active: Boolean = false, val items: List<IosShelfItem> = emptyList(), val loading: Boolean = false,
    val failed: Boolean = false, val visible: Boolean = true, val finishingId: String? = null, val arrivingId: String? = null,
    val grams: Int? = null, val roasters: Int = 0, val undo: Boolean = false, val failure: String? = null,
    val item: IosShelfItem? = null, val missing: Boolean = false, val archived: Boolean = false, val deleting: Boolean = false,
    val busy: Boolean = false, val brewing: Boolean = false, val brewsLoading: Boolean = false, val brewsFailed: Boolean = false,
    val leftGrams: Int? = null, val brews: List<IosShelfBrew> = emptyList(), val visibility: Boolean = false,
    val visibilityEnabled: Boolean = false, val visibilityLoading: Boolean = false, val visibilityFailed: Boolean = false,
    val revision: Int = 0, val add: IosShelfAddState? = null,
)

data class IosShelfAddState(
    val step: String, val scanner: String, val downloadBytes: Long, val downloadDone: Long, val downloadTotal: Long,
    val frame: List<IosShelfPoint>, val stages: List<IosShelfStage>, val failure: String?, val query: String, val roaster: String, val grams: Int?,
    val grind: String?, val purpose: String?, val roastedOn: String?, val today: String, val searching: Boolean,
    val searchFailed: Boolean, val options: List<IosShelfCoffee>, val saving: Boolean, val duplicate: Boolean,
    val landing: Boolean, val hints: List<String>, val message: String?,
)

internal fun ShelfNativeItem.iosItem() = IosShelfItem(id, title, roaster, grams, grind, purpose, roastedOn, addedOn,
    finishedOn, days, roastDays, rating, buyAgain, hasPhoto, hasCoffee)

internal fun ShelfNativeSnapshot.iosState(visibility: ShelfNativeVisibilitySnapshot?) = IosShelfState(
    active = true, items = items.map { it.iosItem() }, loading = loading || (!loaded && !failed), failed = failed,
    finishingId = finishingId, arrivingId = arrivingId, grams = grams, roasters = roasters, undo = undo, failure = failure,
    visibility = visibility?.visible == true, visibilityEnabled = visibility?.enabled == true,
    visibilityLoading = visibility?.loading == true, visibilityFailed = visibility?.failed == true,
)

internal fun ShelfNativeBagSnapshot.iosState() = IosShelfState(
    active = true, item = item?.iosItem(), loading = item == null && !missing && !failed, missing = missing, failed = failed,
    archived = archived, finishingId = item?.id.takeIf { finishing }, deleting = deleting, busy = busy, brewing = brewing,
    failure = failure, brewsLoading = brewsLoading, brewsFailed = brewsFailed, leftGrams = leftGrams,
    brews = brews.map { IosShelfBrew(it.id, it.date, it.method, it.dose, it.score) },
)

internal fun ShelfNativePublicSnapshot.iosState() = IosShelfState(active = true, loading = loading, failed = failed,
    visible = visible, revision = revision, items = items.map {
        IosShelfItem(it.id, it.title, it.roaster, null, null, null, null, null, null, null, null, null, null, it.hasPhoto, it.hasCoffee)
    })

internal fun ShelfNativeAddSnapshot.iosState() = IosShelfState(active = true, add = IosShelfAddState(
    step, scanner, downloadBytes, downloadDone, downloadTotal, frame.map { IosShelfPoint(it.x, it.y) }, stages.map { IosShelfStage(it.name, it.status, it.reasons) },
    failure, query, roaster, grams, grind, purpose, roastedOn, today, searching, searchFailed,
    options.map { IosShelfCoffee(it.slug, it.title, it.roaster) }, saving, duplicate, landing, hints, message,
))
