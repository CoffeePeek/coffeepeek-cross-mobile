package com.coffeepeek.admin.ui.screen.profile

import com.coffeepeek.core.session.SessionReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** One account-scoped presentation snapshot; no check-in entities or image bytes survive here. */
internal data class PersonalProfileWeekSnapshot(
    val counts: Map<String, Int>,
    val photoUrls: Map<String, List<String>>,
)

internal class PersonalProfileWeekCache(private val sessions: SessionReader, scope: CoroutineScope) {
    private data class Entry(val account: String, val today: String, val snapshot: PersonalProfileWeekSnapshot)
    private var currentAccount: String? = null
    private var entry: Entry? = null

    private val watchJob = scope.launch { sessions.observeSession().collect { currentScope() } }

    fun close() { watchJob.cancel(); entry = null; currentAccount = null }

    fun read(account: String, today: String): PersonalProfileWeekSnapshot? {
        if (currentScope() != account) return null
        return entry?.takeIf { it.account == account && it.today == today }?.snapshot?.copyDetached()
    }

    fun write(account: String, today: String, snapshot: PersonalProfileWeekSnapshot) {
        if (currentScope() == account) entry = Entry(account, today, snapshot.copyDetached())
    }

    private fun currentScope(): String? {
        val session = sessions.peekSession()
        val scope = session?.takeIf(sessions::isActiveSession)?.userId?.takeIf(String::isNotBlank)?.let {
            "${sessions.accountGeneration()}:$it"
        }
        if (scope != currentAccount) { currentAccount = scope; entry = null }
        return scope
    }
}

private fun PersonalProfileWeekSnapshot.copyDetached() = PersonalProfileWeekSnapshot(
    counts.toMap(), photoUrls.mapValues { (_, urls) -> urls.toList() },
)
