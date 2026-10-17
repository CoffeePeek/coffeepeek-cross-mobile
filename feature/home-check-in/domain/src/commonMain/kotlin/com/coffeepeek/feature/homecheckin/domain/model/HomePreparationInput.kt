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
    val doseGrams: String? = null,
    val brewDurationSeconds: Int? = null,
    val tasteScore: Int? = null,
    val text: String? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val photos: List<HomePreparationUpload> = emptyList(),
    val drinkPhoto: HomePreparationUpload? = null,
    val visibility: HomePreparationVisibility? = HomePreparationVisibility.Private,
)
