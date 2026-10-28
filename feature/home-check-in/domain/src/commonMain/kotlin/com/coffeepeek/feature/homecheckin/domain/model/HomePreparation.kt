package com.coffeepeek.feature.homecheckin.domain.model

/** Owner read model. A preparation can produce multiple servings; never infer one cup. */
data class HomePreparation(
    val id: String,
    val preparedOn: String,
    val preparedAtUtc: String,
    val createdAtUtc: String,
    val text: String?,
    val brewMethodCode: String?,
    val coffeeProductId: String?,
    val beanName: String?,
    val doseGrams: String?,
    val brewDurationSeconds: Int?,
    val tasteScore: Int?,
    val drinkName: String?,
    val drinkSlug: String?,
    val customDrinkName: String?,
    val photos: List<HomePreparationPhoto>,
    /** Null means unknown, not Private or confirmed Public. */
    val visibility: HomePreparationVisibility?,
    val moderation: HomePreparationModeration,
    /** The owner's shelf bag the bean came from; the bag may since have been deleted. */
    val shelfBagId: String? = null,
    /** Roaster snapshot from the catalogue or the bag. */
    val roasterName: String? = null,
    /** The catalogue coffee's current public slug while it is published; the link key stays [coffeeProductId]. */
    val coffeeProductSlug: String? = null,
    /** This entry's own copy of its bag's card, present while the author shows the bag with it. */
    val shelfBagPhoto: HomePreparationPhoto? = null,
)

data class HomePreparationPhoto(val id: String, val url: String,
    val thumbnailUrl: String? = null, val detailUrl: String? = null, val calendarUrl: String? = null)

enum class HomePreparationVisibility { Private, Public }
enum class HomePreparationModeration { NotSubmitted, Pending, Approved, Rejected, Unknown }
