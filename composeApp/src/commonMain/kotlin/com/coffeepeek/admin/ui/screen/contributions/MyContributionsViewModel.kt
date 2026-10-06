package com.coffeepeek.admin.ui.screen.contributions

import com.coffeepeek.admin.base.BaseViewModel
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shopchange.hint
import com.coffeepeek.admin.ui.screen.shopchange.title
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.domain.model.ModerationStatus
import com.coffeepeek.domain.model.PagedResult
import com.coffeepeek.domain.model.Review
import com.coffeepeek.domain.model.ShopChangeSection
import com.coffeepeek.domain.repository.ReviewRepository
import com.coffeepeek.domain.repository.RoasterRepository
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.domain.repository.ShopChangeRequestRepository
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

enum class ContributionKind(val title: String, val emptyText: String) {
    Reviews("Мои отзывы", "Вы ещё не оставляли отзывов"),
    Shops("Отправленные кофейни", "Вы ещё не добавляли кофейни"),
    Roasters("Отправленные обжарщики", "Вы ещё не добавляли обжарщиков"),
    Changes("Мои правки", "Вы ещё не отправляли правки"),
}

/** Tab order on screen. */
val CONTRIBUTION_TABS = listOf(ModerationStatus.Approved, ModerationStatus.Pending, ModerationStatus.Rejected)

data class ContributionItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val meta: String? = null,
    val changeSection: ShopChangeSection? = null,
    val rejectedReason: String? = null,
    val target: Navigator.Screen? = null,
    /** Reviews render as review cards instead of the generic row. */
    val review: Review? = null,
    val editable: Boolean = false,
)

data class ContributionTab(
    val items: List<ContributionItem> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)

data class MyContributionsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val tabs: Map<ModerationStatus, ContributionTab> = emptyMap(),
) {
    /** Only statuses that actually have items get a tab. */
    val visibleTabs: List<ModerationStatus>
        get() = CONTRIBUTION_TABS.filter { tabs[it]?.items?.isNotEmpty() == true }
}

class MyContributionsViewModel(
    private val kind: ContributionKind,
    private val reviewRepository: ReviewRepository,
    private val shopRepository: ShopRepository,
    private val roasterRepository: RoasterRepository,
    private val changeRepository: ShopChangeRequestRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(MyContributionsUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    /** Loads the first page of every status at once: tab visibility depends on all three. */
    fun refresh() {
        workScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val results = coroutineScope {
                CONTRIBUTION_TABS.map { status -> async { status to fetch(status, page = 1) } }.awaitAll()
            }
            val failure = results.firstNotNullOfOrNull { it.second.exceptionOrNull() }
            if (failure != null) {
                _state.update { it.copy(isLoading = false, error = failure.message ?: "Ошибка загрузки") }
                return@launch
            }
            _state.value = MyContributionsUiState(
                isLoading = false,
                tabs = results.associate { (status, result) -> status to result.getOrThrow().toTab() },
            )
        }
    }

    fun loadMore(status: ModerationStatus) {
        val tab = _state.value.tabs[status] ?: return
        if (!tab.hasMore || tab.isLoadingMore) return
        updateTab(status) { it.copy(isLoadingMore = true) }
        workScope.launch {
            fetch(status, tab.page + 1)
                .onSuccess { page ->
                    updateTab(status) { it.copy(items = it.items + page.items).withPage(page) }
                }
                .onFailure { updateTab(status) { it.copy(isLoadingMore = false) } }
        }
    }

    private fun updateTab(status: ModerationStatus, transform: (ContributionTab) -> ContributionTab) {
        _state.update { state ->
            val tab = state.tabs[status] ?: return@update state
            state.copy(tabs = state.tabs + (status to transform(tab)))
        }
    }

    private suspend fun fetch(status: ModerationStatus, page: Int): Result<PagedResult<ContributionItem>> =
        when (kind) {
            // Published reviews come from the public endpoint: that's what the edit flow and helpful counts key on.
            ContributionKind.Reviews -> if (status == ModerationStatus.Approved) {
                requireAuthSession(sessionRepository)
                    ?: return Result.failure(IllegalStateException("Войдите, чтобы увидеть свои отзывы"))
                val profile = userRepository.getMe().getOrElse { return Result.failure(it) }
                val userId = profile.address?.slug
                    ?: return Result.failure(IllegalStateException("Публичный адрес профиля пока недоступен"))
                reviewRepository.getUserReviews(userId, page, PAGE_SIZE).mapItems {
                    ContributionItem(id = it.id, title = it.header, review = it, editable = true)
                }
            } else {
                reviewRepository.getMyReviewSubmissions(status, page, PAGE_SIZE).mapItems {
                    ContributionItem(
                        id = it.review.id,
                        title = it.review.header,
                        rejectedReason = it.rejectedReason,
                        review = it.review,
                    )
                }
            }

            ContributionKind.Shops -> shopRepository.getMyShopSubmissions(status, page, PAGE_SIZE).mapItems {
                ContributionItem(
                    id = it.id,
                    title = it.name,
                    subtitle = it.address,
                    // Approved but not yet created in the catalog: nothing to open yet.
                    meta = if (it.status == ModerationStatus.Approved && it.publishedShopId == null) {
                        "Скоро появится в каталоге"
                    } else null,
                    rejectedReason = it.rejectedReason,
                    target = it.publishedShopId?.let { id -> Navigator.Screen.ShopDetail(id) },
                )
            }

            // ponytail: no link to the published roaster, ModerationRoasterDto has no published id.
            ContributionKind.Roasters -> roasterRepository.getMyRoasterSubmissions(status, page, PAGE_SIZE).mapItems {
                ContributionItem(
                    id = it.id,
                    title = it.name,
                    subtitle = it.about,
                    rejectedReason = it.rejectedReason,
                )
            }

            ContributionKind.Changes -> changeRepository.getMine(page, PAGE_SIZE, status = status).mapItems {
                ContributionItem(
                    id = it.id,
                    title = it.section.title(),
                    subtitle = it.payload.summary(it.section.hint()),
                    meta = utcIsoToLocalDateTime(it.createdAtUtc),
                    changeSection = it.section,
                    rejectedReason = it.rejectionReason,
                    target = Navigator.Screen.ShopChangeRequestDetail(requestId = it.id),
                )
            }
        }
}

private fun com.coffeepeek.domain.model.ShopChangePayload.summary(fallback: String): String {
    description?.let { return it.ifBlank { "Описание очищено" } }
    if (contacts != null) return "Телефон, почта, сайт и Instagram"
    photos?.let { return "Фото: ${it.retainedPhotoIds.size} сохранено · ${it.newPhotos.size} добавлено" }
    tagIds?.let { return "Выбрано особенностей: ${it.size}" }
    roasterIds?.let { return "Выбрано обжарщиков: ${it.size}" }
    equipmentIds?.let { return "Выбрано оборудования: ${it.size}" }
    brewMethodIds?.let { return "Выбрано методов: ${it.size}" }
    menu?.let { return "Меню: ${it.items.size} позиций · ${it.newPhotos.size} новых фото" }
    return fallback
}

private fun <T> Result<PagedResult<T>>.mapItems(transform: (T) -> ContributionItem) =
    map { PagedResult(it.items.map(transform), it.totalCount, it.totalPages, it.currentPage) }

private fun PagedResult<ContributionItem>.toTab() = ContributionTab(items = items).withPage(this)

private fun ContributionTab.withPage(page: PagedResult<ContributionItem>) = copy(
    totalCount = page.totalCount,
    page = page.currentPage,
    hasMore = page.currentPage < page.totalPages,
    isLoadingMore = false,
)
