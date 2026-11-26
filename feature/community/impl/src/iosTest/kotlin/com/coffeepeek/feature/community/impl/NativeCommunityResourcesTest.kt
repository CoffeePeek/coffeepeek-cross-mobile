@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.feature.community.impl

import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.ui.NativeCommunityResources
import com.coffeepeek.feature.community.impl.ui.previewPost
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/** Uses the actual packaged native resources; no UI, account or platform share service. */
class NativeCommunityResourcesTest {
    @Test fun rawShareTemplatesRetainTheirPositionalArguments() = runTest {
        val resources = NativeCommunityResources("ru")
        val shop = resources.text(Res.string.community_share_shop)
        val home = resources.text(Res.string.community_share_home)
        listOf("%1\u0024s", "%2\u0024s", "%3\u0024s").forEach { assertContains(shop, it) }
        listOf("%1\u0024s", "%2\u0024s").forEach { assertContains(home, it) }
    }

    @Test fun explicitShareArgumentsStillUseTheFormattingOverload() = runTest {
        val text = NativeCommunityResources("ru").text(
            Res.string.community_share_shop, "Ada", "Fixture", "https://example.invalid/post")
        listOf("Ada", "Fixture", "https://example.invalid/post").forEach { assertContains(text, it) }
        listOf("%1\u0024s", "%2\u0024s", "%3\u0024s").forEach { assertFalse(it in text) }
    }

    @Test fun feedSharerLoadsRealTemplatesAndSubstitutesOnlyWhenSharing() = runTest {
        val shared = mutableListOf<String>()
        val sharer = NativeCommunityResources("ru").sharer(shared::add)
        val post = previewPost().let { it.copy(author = it.author.copy(name = "Ada"), shopName = "Fixture") }
        assertNotNull(sharer.action(post)).invoke()
        val text = shared.single()
        listOf("Ada", "Fixture", "https://coffeepeek.by/check-ins/preview").forEach { assertContains(text, it) }
    }
}
