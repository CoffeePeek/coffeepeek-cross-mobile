package com.coffeepeek.feature.ownership.impl

import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.ownership.domain.model.BusinessKind
import kotlinx.coroutines.*

fun createIosOwnershipActionsSession(entry: BusinessOwnershipEntry, kind: String, id: String, slug: String,
    suggest: () -> Unit, ownerEdit: () -> Unit): IosOwnershipActionsSession {
    return IosOwnershipActionsSession(entry as OwnershipEntry, BusinessKind.valueOf(kind), id, slug, suggest, ownerEdit)
}

/** Reuses the entry's OwnershipAccess; toolbar rendering never creates a second ViewModel or access reader. */
class IosOwnershipActionsSession internal constructor(private val entry: OwnershipEntry, val kind: BusinessKind,
    private val id: String, private val slug: String, private val suggest: () -> Unit, private val ownerEdit: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    fun owner() = entry.profileEditorId(kind, id, slug) != null
    fun signedIn() = entry.nativeSignedIn()
    fun observe(receive: () -> Unit) = scope.launch { entry.accountSwitchState.collect { receive() } }
    fun edit() {
        val editor = entry.profileEditorId(kind, id, slug) ?: return
        ownerEdit()
        entry.nativeEdit(kind.name, editor)
    }
    fun suggest() { if (!owner() && kind == BusinessKind.CoffeeShop) suggest.invoke() }
    fun requestClaim(): Boolean {
        if (!signedIn()) { entry.nativeLogin(); return false }
        return !owner()
    }
    fun close() = scope.cancel()
}
