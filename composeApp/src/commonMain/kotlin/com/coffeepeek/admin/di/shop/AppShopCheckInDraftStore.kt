package com.coffeepeek.admin.di.shop

import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.shop.domain.model.ShopCheckInCreateInput
import com.coffeepeek.feature.shop.domain.repository.SavedShopCheckInDraft
import com.coffeepeek.feature.shop.domain.repository.ShopCheckInDraftStorage
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftSnapshot
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInDraftStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serializes draft writes and keeps each sheet bound to its original account and shop. */
internal class AppShopCheckInDraftStore(
    private val storage: ShopCheckInDraftStorage,
    private val sessions: SessionReader,
) {
    private val operations = Mutex()
    private val leases = mutableMapOf<Pair<String, String>, Any>()

    fun observeLatestShop(accountId: String) = storage.observeLatestShop(accountId)

    fun forShop(shopId: String, shopSlug: String, accountId: String): ShopCheckInDraftStore {
        require(shopId.isNotBlank() && shopSlug.isNotBlank() && accountId.isNotBlank())
        return Binding(shopId, shopSlug, accountId, sessions.accountGeneration())
    }

    private inner class Binding(
        private val shopId: String,
        private val shopSlug: String,
        private val accountId: String,
        private val generation: Long,
    ) : ShopCheckInDraftStore {
        private val key = accountId to shopId
        private val token = Any()

        override suspend fun open(initial: ShopCheckInCreateInput) = operation {
            require(initial.shopSlug == shopSlug)
            val saved = storage.read(accountId, shopId)
            leases[key] = token
            ShopCheckInDraftSnapshot(
                saved?.input?.copy(shopSlug = shopSlug) ?: initial,
                saved?.deliveryUnconfirmed == true,
                exists = saved != null,
            )
        }

        override suspend fun save(snapshot: ShopCheckInDraftSnapshot) = operation {
            checkBinding(snapshot.input.shopSlug)
            storage.save(accountId, SavedShopCheckInDraft(shopId, snapshot.input, snapshot.deliveryUnconfirmed))
        }

        override suspend fun clear(shopSlug: String) = operation {
            checkBinding(shopSlug)
            storage.delete(accountId, shopId)
            leases.remove(key)
            Unit
        }

        private fun checkBinding(slug: String) {
            check(slug == shopSlug && leases[key] === token) { "Draft binding was replaced" }
        }

        private suspend fun <T> operation(block: suspend () -> T): Result<T> =
            withContext(NonCancellable) {
                operations.withLock {
                    try {
                        val session = sessions.peekSession()
                        check(session?.userId == accountId && sessions.isActiveSession(session) &&
                            sessions.accountGeneration() == generation) { "Draft account was replaced" }
                        Result.success(block())
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Result.failure(error)
                    }
                }
            }
    }
}
