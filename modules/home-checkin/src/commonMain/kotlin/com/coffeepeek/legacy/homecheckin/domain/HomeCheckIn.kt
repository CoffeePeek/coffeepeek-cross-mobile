package com.coffeepeek.feature.homecheckin.domain

enum class HomeCheckInVisibility { Private, Public }

enum class HomeCheckInModeration { NotSubmitted, Pending, Approved, Rejected, Unknown }

/** One preparation, including a batch that produces several servings. */
data class HomeCheckIn(
    val id: String,
    val preparedAtUtc: String,
    val preparedOn: String,
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
    val photos: List<HomeCheckInPhoto>,
    /** Null means the server has not supplied visibility; never infer publication. */
    val visibility: HomeCheckInVisibility? = null,
    val moderation: HomeCheckInModeration = HomeCheckInModeration.Unknown,
)

data class HomeCheckInPhoto(val id: String, val url: String, val thumbnailUrl: String = url,
    val detailUrl: String = url, val calendarUrl: String = thumbnailUrl)

data class HomePhotoUpload(val fileName: String, val contentType: String, val bytes: ByteArray)

data class HomeCheckInInput(
    val preparedAt: String,
    val preparedOn: String? = null,
    val brewMethodCode: String? = null,
    val coffeeProductSlug: String? = null,
    /** Only valid on an edit of the same entry; preserves its historical bean name. */
    val coffeeProductId: String? = null,
    val customBeanName: String? = null,
    val doseGrams: String? = null,
    val brewDurationSeconds: Int? = null,
    val tasteScore: Int? = null,
    val text: String? = null,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val photos: List<HomePhotoUpload> = emptyList(),
    /** Null on an edit preserves an unknown server value until the user chooses. */
    val visibility: HomeCheckInVisibility? = HomeCheckInVisibility.Private,
)

data class HomeCheckInPage(val items: List<HomeCheckIn>, val totalCount: Int, val page: Int, val pageSize: Int) {
    val hasMore: Boolean get() = page.toLong() * pageSize < totalCount
}

data class HomeCalendarDay(val preparedOn: String, val brewCount: Int)

data class HomeBeanStats(
    val coffeeProductId: String?, val beanName: String?, val brewCount: Int,
    val dosedBrewCount: Int, val totalKnownDoseGrams: String,
    val ratedBrewCount: Int, val averageTasteScore: Double?,
)

data class HomeMethodStats(
    val brewMethodCode: String?, val brewCount: Int, val dosedBrewCount: Int,
    val totalKnownDoseGrams: String, val ratedBrewCount: Int, val averageTasteScore: Double?,
)

interface HomeCheckInRepository {
    suspend fun create(input: HomeCheckInInput): Result<HomeCheckIn>
    suspend fun history(page: Int = 1, pageSize: Int = 20, from: String? = null, to: String? = null): Result<HomeCheckInPage>
    suspend fun calendar(from: String, to: String): Result<List<HomeCalendarDay>>
    suspend fun detail(id: String): Result<HomeCheckIn>
    suspend fun update(id: String, input: HomeCheckInInput): Result<HomeCheckIn>
    suspend fun delete(id: String): Result<Unit>
    suspend fun beanStats(): Result<List<HomeBeanStats>>
    suspend fun methodStats(): Result<List<HomeMethodStats>>
    /** Downloads protected bytes through the same authenticated client as other owner routes. */
    suspend fun photo(url: String): Result<ByteArray>
}
