package com.coffeepeek.feature.homecheckin.domain.model

/** Create/edit values, independent of transport. Null edit visibility preserves an unknown value. */
data class HomePreparationInput(
    val preparedAt: String,
    val preparedOn: String? = null,
    val brewMethodCode: String? = null,
    val coffeeProductSlug: String? = null,
    /** Historical identity: allowed only when editing that preparation. */
    val coffeeProductId: String? = null,
    val customBeanName: String? = null,
    /** The owner's shelf bag; the server takes the bean from it. Exclusive with the other bean fields. */
    val shelfBagId: String? = null,
    /** Show a copy of the bag's card with the entry; ignored without a bag or when the bag has no card. */
    val showShelfBag: Boolean = true,
    val doseGrams: String? = null,
    val brewDurationSeconds: Int? = null,
    val tasteScore: Int? = null,
    val text: String? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val photos: List<HomePreparationUpload> = emptyList(),
    val visibility: HomePreparationVisibility? = HomePreparationVisibility.Public,
)
