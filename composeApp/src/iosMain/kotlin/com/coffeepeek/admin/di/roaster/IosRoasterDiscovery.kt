package com.coffeepeek.admin.di.roaster

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.roaster.impl.NativeRoasterDiscoveryHost
import com.coffeepeek.feature.roaster.impl.NativeRoasterListSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class IosRoasterDiscovery internal constructor(
    private val session: NativeRoasterListSession,
    private var host: NativeRoasterDiscoveryHost,
    language: LanguagePort, accounts: SessionReader, media: IosCatalogueMedia,
) : IosRoasterScreen(language, accounts, media::observeImage) {
    val search = host.search
    private val hostRevision = MutableStateFlow(0L)
    fun current(): IosRoasterDiscoveryState {
        val state = session.current()
        val active = isCurrent()
        val cards = state.cards.takeIf { active }.orEmpty().let { if (search) it else it.take(8) }
        return IosRoasterDiscoveryState(active, state.loading && active, state.failed && active, state.query.takeIf { active }.orEmpty(), host.bottomClearance,
            cards.map { IosRoasterCard(it.slug, it.name, it.photo, it.about, it.shops, it.products, it.favorite) },
            state.tags.takeIf { active }.orEmpty().map { IosRoasterTag(it.id, it.name, it.selected) },
            state.messageKey.takeIf { active }, state.message.takeIf { active })
    }
    fun observeState(receive: (IosRoasterDiscoveryState) -> Unit) = observe(combine(session.state, hostRevision) { _, _ -> Unit }) { receive(current()) }
    fun observeEvents() = IosRoasterObservation(scope.launch {
        session.opened.collect { slug -> guarded { host.onOpen(slug) } }
    })
    fun retry() = guarded(session::retry)
    fun open(slug: String) = guarded { session.open(slug) }
    fun appeared(slug: String) = guarded { session.appeared(slug) }
    fun favorite(slug: String) = guarded { host.onFavoriteFeedback(); session.favorite(slug) }
    fun tag(id: String) = guarded { session.tag(id) }
    fun clearTags() = guarded(session::clearTags)
    fun dismissMessage() = guarded(session::dismissMessage)
    fun query(value: String) = guarded { host.onQuery(value) }
    fun add() = guarded(host.onAdd)
    fun cancel() = guarded(host.onCancel)
    internal fun update(next: NativeRoasterDiscoveryHost) { host = next; hostRevision.value++ }
}

data class IosRoasterCard(
    val slug: String?, val name: String, val photo: String?, val about: String?,
    val shops: Int, val products: Int, val favorite: Boolean,
)
data class IosRoasterTag(val id: String, val name: String, val selected: Boolean)
data class IosRoasterDiscoveryState(
    val active: Boolean, val loading: Boolean, val failed: Boolean, val query: String, val bottomClearance: Float,
    val cards: List<IosRoasterCard>, val tags: List<IosRoasterTag>, val messageKey: String?, val message: String?,
)
