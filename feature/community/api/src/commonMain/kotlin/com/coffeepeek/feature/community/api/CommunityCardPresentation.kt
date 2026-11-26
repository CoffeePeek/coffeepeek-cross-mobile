package com.coffeepeek.feature.community.api

/** Read-only presentation for a platform card. Feature UI owns all labels and allowed callbacks. */
data class CommunityCardPresentation(
    val key: String,
    val header: CommunityCardHeader,
    val body: CommunityCardBody,
    val footer: CommunityCardFooter,
    val status: CommunityCardStatus,
    val menu: List<CommunityCardMenuItem>,
    val openLabel: String,
    val menuLabel: String,
    val horizontalInset: Double,
)

data class CommunityCardHeader(val name: String, val avatar: String?, val dateLabel: String, val openAuthorLabel: String)

data class CommunityCardBody(
    val isHome: Boolean, val title: String, val note: String,
    val rating: Double?, val ratingLabel: String,
    val beanName: String?, val roasterName: String?, val bagUrl: String?, val recipe: String,
    val photos: List<CommunityCardPhoto>,
    val recipeSeparator: String, val openCoffeeLabel: String,
)

data class CommunityCardPhoto(val previewUrl: String, val detailUrl: String?, val label: String)
data class CommunityCardFooter(val likes: Int?, val liked: Boolean, val likesLabel: String,
    val drink: String?, val publishLabel: String?)
data class CommunityCardStatus(val publication: String?, val rejectedTitle: String?, val rejectedReason: String?, val editLabel: String?)
data class CommunityCardMenuItem(val id: String, val label: String, val enabled: Boolean, val destructive: Boolean = false)

/** A renderer may only invoke offered actions; the original feature callbacks retain their guards. */
class CommunityCardActions(val actions: Map<String, () -> Unit>, val onPhoto: (Int) -> Unit)
