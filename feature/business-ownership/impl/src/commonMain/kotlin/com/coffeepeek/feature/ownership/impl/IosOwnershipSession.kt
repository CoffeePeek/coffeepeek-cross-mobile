package com.coffeepeek.feature.ownership.impl

import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import kotlinx.coroutines.launch

/** Platform-independent native presentation session; controls dispatch to the existing feature model. */
class IosOwnershipSession internal constructor(private val model: OwnershipViewModel, private val account: OwnershipAccount,
    val tab: Boolean, val claimSheet: Boolean, private val choosePhotos: () -> Unit,
    private val checkInLink: (String) -> String?, private val shareQr: (ByteArray, String) -> Unit,
    private val open: (String, String) -> Unit, private val dispose: () -> Unit = {}) {
    private var closed = false
    fun current(): OwnershipNativeSnapshot {
        val state = model.state.value
        return OwnershipNativeSnapshot(if (!closed && state.account == account.current()) state else OwnershipState(generation = state.generation), tab)
    }
    fun observe(receive: () -> Unit) = model.viewModelScope.launch { model.state.collect { receive() } }
    fun isCurrent(generation: Int) = !closed && model.state.value.let { it.generation == generation && it.account == account.current() }
    fun close() { if (!closed) { closed = true; dispose() } }
    fun action(value: String, generation: Int) {
        if (!isCurrent(generation)) return
        val action = when (value) {
            "refresh" -> OwnershipAction.Refresh
            "login" -> OwnershipAction.Login
            "newClaim" -> OwnershipAction.NewClaim
            "submit" -> OwnershipAction.Submit
            "askSave" -> OwnershipAction.AskSave
            "save" -> OwnershipAction.Save
            "cancel" -> OwnershipAction.CancelConfirmation
            "discard" -> OwnershipAction.Discard
            "askReload" -> OwnershipAction.AskReload
            "reload" -> OwnershipAction.Reload
            "retry" -> retry()
            else -> OwnershipAction.Back
        }
        model.onAction(action)
    }
    private fun retry(): OwnershipAction = model.state.value.let {
        when {
            it.reloadRequired && it.profile != null -> OwnershipAction.AskReload
            it.destination == OwnershipDestination.Submit -> OwnershipAction.Submit
            it.destination == OwnershipDestination.Editor && it.profile != null -> OwnershipAction.AskSave
            else -> OwnershipAction.Refresh
        }
    }
    fun claim(id: String, generation: Int) {
        if (isCurrent(generation) && model.state.value.claims?.items?.any { it.id == id } == true) model.onAction(OwnershipAction.OpenClaim(id))
    }
    fun business(kind: String, id: String, generation: Int) {
        if (!isCurrent(generation)) return
        val found = (model.state.value.shops + model.state.value.roasters?.items.orEmpty()).firstOrNull { it.id == id && it.kind.name == kind } ?: return
        if (tab) open(kind, id) else model.onAction(OwnershipAction.OpenBusiness(found.kind, id))
    }
    fun claimsPage(page: Int, generation: Int) { if (isCurrent(generation)) model.onAction(OwnershipAction.ClaimsPage(page)) }
    fun roastersPage(page: Int, generation: Int) { if (isCurrent(generation)) model.onAction(OwnershipAction.RoastersPage(page)) }
    fun claimText(field: String, value: String, generation: Int) {
        val target = ClaimField.entries.firstOrNull { it.name == field } ?: return
        if (isCurrent(generation)) model.onAction(OwnershipAction.ClaimText(target, value))
    }
    fun profileText(field: String, value: String, generation: Int) {
        val target = ProfileField.entries.firstOrNull { it.name == field } ?: return
        if (isCurrent(generation)) model.onAction(OwnershipAction.ProfileText(target, value))
    }
    fun choosePhotos(generation: Int) {
        val state = model.state.value
        if (isCurrent(generation) && state.account != null && !state.busy && !state.reloadRequired) choosePhotos()
    }
    fun removePhoto(index: Int, evidence: Boolean, generation: Int) {
        if (isCurrent(generation)) model.onAction(if (evidence) OwnershipAction.RemoveEvidence(index) else OwnershipAction.RemovePhoto(index))
    }
    fun movePhoto(from: Int, to: Int, evidence: Boolean, generation: Int) {
        if (isCurrent(generation)) model.onAction(if (evidence) OwnershipAction.MoveEvidence(from, to) else OwnershipAction.MovePhoto(from, to))
    }
    fun photo(key: String, generation: Int): OwnershipNativePhotoSource? {
        if (!isCurrent(generation) || model.state.value.account == null) return null
        return nativePhoto(model.state.value, key)?.let { OwnershipNativePhotoSource(it.url, it.bytes) }
    }
    fun qrLink(id: String, generation: Int): String? {
        if (!isCurrent(generation)) return null
        val shop = model.state.value.shops.firstOrNull { it.id == id } ?: return null
        return shop.slug?.let(checkInLink)
    }
    fun shareQr(bytes: ByteArray, name: String, generation: Int) { if (isCurrent(generation)) shareQr(bytes, name) }
}

class OwnershipNativePhotoSource internal constructor(val url: String?, val bytes: ByteArray?)

private fun nativePhoto(state: OwnershipState, key: String): BusinessPhoto? {
    return when {
        key.startsWith("draft:") -> state.draft?.photos?.firstOrNull { "draft:${it.key}" == key }
        key.startsWith("evidence:") -> state.submission.photos.firstOrNull { "evidence:${it.key}" == key }
        key.startsWith("claim:") -> state.selectedClaim?.evidencePhotos?.firstOrNull { "claim:${it.key}" == key }
        else -> state.cardProfiles.entries.firstOrNull { "card:${it.key.first.name}:${it.key.second}:${it.value.revision}" == key }?.value?.photos?.firstOrNull()
    }
}
