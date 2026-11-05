package com.coffeepeek.admin.di.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Navigation-owned lifetime. Session changes clear feature VMs even while a child route covers them. */
internal class ShopBrowsingHostViewModel(
    private val sessions: SessionReader,
    private val visibility: AuthorVisibility = AuthorVisibility.None,
) : ViewModel() {
    private val mutableSession = MutableStateFlow(ShopBrowsingHostSession(0, ShopBrowsingSession(sessions, visibility = visibility)))
    val session = mutableSession.asStateFlow()
    private var observation: Job? = null

    init { observeSession() }

    fun retry() {
        observation?.cancel()
        replaceSession(sessions.peekSession())
        observeSession()
    }

    private fun observeSession() {
        observation = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            combine(sessions.observeSession(), visibility.visibility) { session, _ -> session }.catch { error ->
                if (error is CancellationException) throw error
                // Never retain private screen content when observing account changes fails.
                replaceSession(null, observationFailed = true)
            }.collect { value ->
                if (!mutableSession.value.lease.isCurrent(value)) replaceSession(value)
            }
        }
    }

    private fun replaceSession(value: com.coffeepeek.domain.model.Session?, observationFailed: Boolean = false) {
        val old = mutableSession.value
        old.lease.revoke()
        old.viewModelStore.clear()
        mutableSession.value = ShopBrowsingHostSession(old.generation + 1,
            ShopBrowsingSession(sessions, value, visibility), observationFailed)
    }

    override fun onCleared() {
        mutableSession.value.lease.revoke()
        mutableSession.value.viewModelStore.clear()
    }
}

internal class ShopBrowsingHostSession(
    val generation: Int,
    val lease: ShopBrowsingSession,
    val observationFailed: Boolean = false,
) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
