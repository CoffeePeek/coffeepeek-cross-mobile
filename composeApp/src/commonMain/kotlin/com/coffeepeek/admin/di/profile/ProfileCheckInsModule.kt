package com.coffeepeek.admin.di.profile

import com.coffeepeek.feature.community.api.PublicAuthorPosts
import com.coffeepeek.feature.community.data.repository.createPublicAuthorPosts
import com.coffeepeek.feature.profile.api.ProfileCheckInLikes
import com.coffeepeek.feature.profile.api.PublicCheckInsPort
import com.coffeepeek.feature.profile.data.repository.createPublicProfileCheckIns
import org.koin.dsl.module

internal fun profileCheckInsModule() = module {
    single<PublicAuthorPosts> { createPublicAuthorPosts(get()) }
    single<PublicCheckInsPort> { createPublicProfileCheckIns(get()) }
    single<ProfileCheckInLikes> { AppProfileCheckInLikes(get(), get(), get()) }
}
