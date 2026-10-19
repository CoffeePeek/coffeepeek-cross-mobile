package com.coffeepeek.admin.di.shop

import com.coffeepeek.domain.model.Session
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorReference
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.authorblocking.api.AuthorVisibilitySnapshot
import com.coffeepeek.feature.shop.api.ShopCheckInAuthor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidShopAuthorBlockBridgeTest {
    private val author = ShopCheckInAuthor("public-id", "public-slug", "Author", "issued-avatar")

    private class Actions : AuthorBlockActions {
        val requested = mutableListOf<AuthorReference>()
        override fun requestBlock(author: AuthorReference) { requested += author }
        override fun requestUnblock(author: AuthorReference) = error("No unblock action in check-in menus")
    }

    @Test fun guestsAndSignedInViewersDelegateToTheExistingPromptWithoutWriting() {
        for (sessions in listOf(CheckInTestSessions(null), CheckInTestSessions())) {
            val actions = Actions()
            AndroidShopBrowsingSession(sessions).requestAuthorBlock(author, actions)
            assertEquals(listOf(AuthorReference("public-id", "public-slug", "Author", "issued-avatar")), actions.requested)
        }
    }

    @Test fun tokenRenewalKeepsTheCallbackButAccountReplacementRevokesItPermanently() = runTest {
        val sessions = CheckInTestSessions()
        val lease = AndroidShopBrowsingSession(sessions)
        val actions = Actions()
        sessions.applySession(Session("renewed", userId = "owner"))
        lease.requestAuthorBlock(author, actions)
        assertEquals(1, actions.requested.size)
        sessions.applySession(Session("other", userId = "other"))
        lease.requestAuthorBlock(author, actions)
        sessions.applySession(Session("owner-again", userId = "owner"))
        lease.requestAuthorBlock(author, actions)
        assertEquals(1, actions.requested.size)
    }

    @Test fun visibilityChangeAndEmptyIdentityCannotOpenAPrompt() {
        val visibility = object : AuthorVisibility {
            override val visibility = MutableStateFlow(AuthorVisibilitySnapshot())
            override suspend fun refresh() = Result.success(Unit)
        }
        val lease = AndroidShopBrowsingSession(CheckInTestSessions(), visibility = visibility)
        val actions = Actions()
        lease.requestAuthorBlock(author.copy(id = null, slug = " "), actions)
        assertTrue(actions.requested.isEmpty())
        visibility.visibility.value = AuthorVisibilitySnapshot(revision = 1)
        lease.requestAuthorBlock(author, actions)
        assertTrue(actions.requested.isEmpty())
    }
}
