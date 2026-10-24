package com.coffeepeek.feature.community.domain.model

data class FeedFilters(val citySlug: String? = null, val coffeeShopSlug: String? = null, val authorSlug: String? = null)

data class FeedEntry(val publishedAtUtc: String, val post: CommunityPost)

/** [nextCursor] is opaque; null means the last page. */
data class FeedPage(val entries: List<FeedEntry>, val nextCursor: String?)

data class MyPostsPage(val posts: List<CommunityPost>, val hasMore: Boolean)

data class HelpfulVote(val isHelpful: Boolean, val helpfulCount: Int)

data class PostEdit(val text: String, val rating: PostRating? = null, val drinkSlug: String? = null, val customDrinkName: String? = null)

data class DrinkOption(val slug: String, val nameRu: String, val nameEn: String)

/** The feed could not be read; [restartPagination] means the cursor is no longer valid and the list must start over. */
class FeedLoadException(message: String, val restartPagination: Boolean) : Exception(message)

enum class FailureKind { SignInRequired, Forbidden, NotFound, Other }

/** A mutation was refused or failed; [serverMessage] is the server's own text when it sent one. */
class CommunityFailure(val kind: FailureKind, val serverMessage: String? = null) : Exception(serverMessage)
