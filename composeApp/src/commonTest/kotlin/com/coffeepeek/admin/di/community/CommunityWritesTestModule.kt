package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.CommunityAccount
import com.coffeepeek.feature.community.api.CommunitySavedPosts
import com.coffeepeek.feature.community.impl.createCommunitySavedPosts
import kotlinx.coroutines.flow.emptyFlow
import org.koin.dsl.module

internal fun communityWritesTestModule() = module {
    single<CommunityAccount> { object : CommunityAccount {
        override val accountKey = emptyFlow<String?>()
        override fun currentAccountKey(): String? = null
        override suspend fun ownAuthorSlug(): Result<String?> = error("Unexpected account read")
    } }
    single<CommunitySavedPosts> { createCommunitySavedPosts() }
}
