package com.coffeepeek.admin.di.community

import platform.UIKit.UIViewController

private lateinit var feedFactory: (IosCommunityFeed) -> UIViewController
internal fun configureNativeCommunityFeed(factory: (IosCommunityFeed) -> UIViewController) { feedFactory = factory }
internal fun communityFeedController(feed: IosCommunityFeed) = feedFactory(feed)
