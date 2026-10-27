package com.coffeepeek.feature.contributions.impl.change

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.domain.model.Availability
import com.coffeepeek.feature.contributions.domain.model.ChangeRequest
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ContributionFailure
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.FailureKind
import com.coffeepeek.feature.contributions.domain.model.MenuEntry
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The editor of one section of a shop, new or an own request that is not approved yet. It starts from the shop as it is now and, when editing, lays the
 * request's payload over it. The rule over the legacy: one account at a time. A guest sees a sign-in prompt, a change of account starts an empty
 * editor, and late answers of a previous account are dropped; a failed load or send shows the server message or a default text.
 */
internal class ChangeEditorViewModel(
    private val shopSlug: String,
    private val section: ChangeSection,
    private val requestId: String?,
    private val repository: ContributionsRepository,
    private val catalogs: ContributionCatalogs,
    private val account: ContributionsAccount,
) : MviViewModel<ChangeEditorState, ChangeEditorAction, ChangeEditorEvent>(ChangeEditorState(section)) {
    private var generation = 0L
    private var work: Job? = null

    init {
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { accountKey ->
                generation++
                work?.cancel()
                if (accountKey == null) updateState { ChangeEditorState(section, signedIn = false, isLoading = false) }
                else load()
            }
        }
    }

    override suspend fun handleActionInternal(action: ChangeEditorAction) {
        if (currentState.signedIn != true) return
        if (currentState.busy && action != ChangeEditorAction.DismissFailure) return
        when (action) {
            is ChangeEditorAction.Description -> updateState { copy(description = action.value.take(ContributionLimits.DESCRIPTION)) }
            is ChangeEditorAction.Phone -> updateState { copy(phone = action.value) }
            is ChangeEditorAction.Email -> updateState { copy(email = action.value) }
            is ChangeEditorAction.Website -> updateState { copy(website = action.value) }
            is ChangeEditorAction.Instagram -> updateState { copy(instagram = action.value) }
            is ChangeEditorAction.Toggle -> updateState {
                when (section) {
                    ChangeSection.Tags -> copy(tagIds = tagIds.toggle(action.id))
                    ChangeSection.Roasters -> copy(roasterIds = roasterIds.toggle(action.id))
                    ChangeSection.Equipment -> copy(equipmentIds = equipmentIds.toggle(action.id))
                    ChangeSection.BrewMethods -> copy(brewMethodIds = brewMethodIds.toggle(action.id))
                    else -> this
                }
            }
            is ChangeEditorAction.AddPhotos -> updateState { copy(newPhotos = newPhotos + action.photos.take((ContributionLimits.SHOP_PHOTOS - photoCount).coerceAtLeast(0))) }
            is ChangeEditorAction.RemoveRetainedPhoto -> updateState { copy(retainedPhotos = retainedPhotos.filterNot { it.id == action.id }) }
            is ChangeEditorAction.RemoveNewPhoto -> updateState { copy(newPhotos = newPhotos.filterIndexed { i, _ -> i != action.index }) }
            ChangeEditorAction.RemoveUploadedPhotos -> updateState { copy(uploadedPhotos = emptyList()) }
            is ChangeEditorAction.AddMenuPhotos -> updateState {
                copy(newMenuPhotos = newMenuPhotos + action.photos.take((ContributionLimits.MENU_PHOTOS - menuPhotoCount).coerceAtLeast(0)))
            }
            is ChangeEditorAction.RemoveRetainedMenuPhoto -> updateState { copy(retainedMenuPhotos = retainedMenuPhotos.filterNot { it.id == action.id }) }
            is ChangeEditorAction.RemoveNewMenuPhoto -> updateState { copy(newMenuPhotos = newMenuPhotos.filterIndexed { i, _ -> i != action.index }) }
            ChangeEditorAction.RemoveUploadedMenuPhotos -> updateState { copy(uploadedMenuPhotos = emptyList()) }
            is ChangeEditorAction.SetAvailability -> updateRow(action.slug) { copy(availability = action.availability) }
            is ChangeEditorAction.SetPrice -> updateRow(action.slug) { copy(priceText = action.text) }
            ChangeEditorAction.Reload -> load()
            ChangeEditorAction.Submit -> submit()
            ChangeEditorAction.DismissFailure -> updateState { copy(submit = EditorSubmit.Idle) }
        }
    }

    private fun updateRow(slug: String, transform: MenuRowState.() -> MenuRowState) =
        updateState { copy(menuRows = menuRows.map { if (it.slug == slug) it.transform() else it }) }

    private fun load() {
        generation++
        work?.cancel()
        val mine = generation
        updateState { ChangeEditorState(section, signedIn = true) }
        work = viewModelScope.launch {
            try {
                val loaded = read()
                if (mine == generation) updateState { loaded(this).copy(isLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (mine == generation) loadFailed(failure)
            }
        }
    }

    private fun loadFailed(failure: Exception) {
        val known = failure as? ContributionFailure
        updateState {
            copy(isLoading = false, loadMessage = known?.serverMessage,
                loadError = when (known?.kind) {
                    FailureKind.SignInRequired -> EditorLoadError.SignInRequired
                    FailureKind.NotFound -> EditorLoadError.NotFound
                    else -> EditorLoadError.Failed
                },
            )
        }
    }

    /** Reads the catalogs, the shop, the drinks (for the menu) and the request being edited; any failure of the required ones fails the load. */
    private suspend fun read(): (ChangeEditorState) -> ChangeEditorState {
        val all = catalogs.catalogs().getOrThrow()
        val shop = catalogs.shop(shopSlug).getOrThrow()
        // A failed drinks read must not look like an empty catalog: the editor would show no rows and send a menu without the saved items.
        val drinks = if (section == ChangeSection.Menu) catalogs.drinks().getOrThrow() else emptyList()
        val request = requestId?.let { repository.change(it).getOrThrow() }
        return { state -> state.fromShop(shop, all, drinks).let { if (request != null) it.fromRequest(request, shop, drinks) else it } }
    }

    private fun submit() {
        val state = currentState
        if (!state.canSubmit) return
        updateState { copy(submit = EditorSubmit.Sending) }
        val mine = generation
        val draft = state.toDraft(shopSlug)
        work = viewModelScope.launch {
            val result = if (requestId == null) repository.submitChange(draft) else repository.editChange(requestId, draft)
            if (mine != generation) return@launch
            result.onSuccess { request -> updateState { copy(submit = EditorSubmit.Done(request)) } }
                .onFailure { failure -> updateState { copy(submit = EditorSubmit.Failed((failure as? ContributionFailure)?.serverMessage)) } }
        }
    }
}

private fun ChangeEditorState.fromShop(shop: ShopSnapshot, all: com.coffeepeek.feature.contributions.domain.model.Catalogs, drinks: List<DrinkOption>) = copy(
    shopTitle = shop.title,
    catalogs = all,
    description = shop.description.orEmpty(),
    phone = shop.contacts.phone.orEmpty(),
    email = shop.contacts.email.orEmpty(),
    website = shop.contacts.website.orEmpty(),
    instagram = shop.contacts.instagram.orEmpty(),
    retainedPhotos = shop.photos,
    tagIds = shop.tagIds.toSet(),
    roasterIds = shop.roasterIds.toSet(),
    equipmentIds = shop.equipmentIds.toSet(),
    brewMethodIds = shop.brewMethodIds.toSet(),
    menuRows = menuRows(drinks, shop.menu.map { MenuEntry(it.slug, it.availability, it.price) }),
    retainedMenuPhotos = shop.menuPhotos,
)

private fun ChangeEditorState.fromRequest(request: ChangeRequest, shop: ShopSnapshot, drinks: List<DrinkOption>): ChangeEditorState {
    val payload = request.payload
    val photosById = shop.photos.associateBy { it.id }
    val menuPhotosById = shop.menuPhotos.associateBy { it.id }
    return copy(
        description = payload.description ?: description,
        phone = payload.contacts?.phone ?: phone,
        email = payload.contacts?.email ?: email,
        website = payload.contacts?.website ?: website,
        instagram = payload.contacts?.instagram ?: instagram,
        retainedPhotos = payload.photos?.retainedIds?.mapNotNull { photosById[it] } ?: retainedPhotos,
        uploadedPhotos = payload.photos?.uploaded.orEmpty(),
        tagIds = payload.tagIds?.toSet() ?: tagIds,
        roasterIds = payload.roasterIds?.toSet() ?: roasterIds,
        equipmentIds = payload.equipmentIds?.toSet() ?: equipmentIds,
        brewMethodIds = payload.brewMethodIds?.toSet() ?: brewMethodIds,
        retainedMenuPhotos = payload.menu?.retainedPhotoIds?.mapNotNull { menuPhotosById[it] } ?: retainedMenuPhotos,
        uploadedMenuPhotos = payload.menu?.uploaded.orEmpty(),
        menuRows = payload.menu?.items?.let { items -> menuRows(drinks, items) } ?: menuRows,
    )
}

private fun DrinkOption.toRow(entry: MenuEntry?) =
    MenuRowState(slug, name, entry?.availability ?: Availability.Unknown, entry?.price?.let(::priceText).orEmpty())

/** A row per drink of the catalog; an entry whose drink left the catalog keeps its row (named by its slug) instead of vanishing silently. */
private fun menuRows(drinks: List<DrinkOption>, entries: List<MenuEntry>): List<MenuRowState> {
    val known = drinks.map { drink -> drink.toRow(entries.firstOrNull { it.slug.equals(drink.slug, ignoreCase = true) }) }
    val retired = entries.filter { entry -> drinks.none { it.slug.equals(entry.slug, ignoreCase = true) } }
        .map { entry -> MenuRowState(entry.slug, entry.slug, entry.availability, entry.price?.let(::priceText).orEmpty()) }
    return known + retired
}

private fun priceText(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id
