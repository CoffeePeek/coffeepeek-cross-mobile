package com.coffeepeek.admin.settings

import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.room.repository.SettingRepository
import com.coffeepeek.room.repository.readSerializable
import com.coffeepeek.room.repository.saveSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

/**
 * Review draft policy
 *
 * Kept: closing the sheet, leaving the shop / visiting other shops, backgrounding the app
 *       (e.g. to check the website), process death, a failed submit.
 * Keyed: one draft per shop for new reviews, one per review for edits — drafts never mix.
 * Saved: automatically, debounced, on every change. Text + ratings go to local storage;
 *        photos stay in memory only (can be megabytes), so they survive navigation but not process death.
 * Cleared: successful submit, explicit «Удалить черновик», logout, or when older than [DRAFT_TTL].
 * Never stored: a draft with nothing entered (see [ReviewDraft.isBlank]).
 */
@Serializable
data class ReviewDraft(
    val header: String = "",
    val comment: String = "",
    val placeRating: Int,
    val serviceRating: Int,
    val coffeeRating: Int,
    val savedAtEpochMs: Long = 0,
    val drinkSlug: String? = null,
    val customDrinkName: String? = null,
    val drinkName: String? = null,
    val drinkNameEn: String? = null,
    val drinkSelectionChanged: Boolean = false,
)

val DRAFT_TTL: Duration = 30.days
private const val KEY_PREFIX = "review_draft:"
private const val SAVE_DEBOUNCE_MS = 400L

/** Blank = the user typed nothing and left ratings at the form's defaults. */
internal fun ReviewDraft.isBlank(defaultRating: Int): Boolean =
    drinkSlug == null && header.isBlank() && comment.isBlank() &&
        placeRating == defaultRating && serviceRating == defaultRating && coffeeRating == defaultRating

internal fun ReviewDraft.isExpired(nowEpochMs: Long, ttl: Duration = DRAFT_TTL): Boolean =
    nowEpochMs - savedAtEpochMs > ttl.inWholeMilliseconds

@OptIn(ExperimentalTime::class)
private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

class ReviewDraftStore(
    private val settingRepository: SettingRepository,
) {
    // Own scope: a pending debounced save must still land after the sheet's ViewModel is cleared.
    // Serial dispatcher: save/clear run in call order, so a save issued just before a clear
    // (e.g. the last keystroke before «Отправить») can never resurrect the draft afterwards.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val serial = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + serial)
    private val pendingSaves = mutableMapOf<String, Job>()
    private val photos = mutableMapOf<String, List<PickedImage>>()

    suspend fun load(key: String): ReviewDraft? {
        val draft = runCatching { settingRepository.readSerializable<ReviewDraft>(key) }.getOrNull() ?: return null
        if (draft.isExpired(nowMs())) {
            clear(key)
            return null
        }
        return draft
    }

    fun photos(key: String): List<PickedImage> = photos[key].orEmpty()

    /** Debounced persist; a blank draft (and no photos) deletes the key instead. */
    fun save(key: String, draft: ReviewDraft, draftPhotos: List<PickedImage>, defaultRating: Int) {
        if (draftPhotos.isEmpty()) photos.remove(key) else photos[key] = draftPhotos
        val blank = draft.isBlank(defaultRating) && draftPhotos.isEmpty()
        scope.launch {
            pendingSaves.remove(key)?.cancel()
            pendingSaves[key] = scope.launch {
                delay(SAVE_DEBOUNCE_MS)
                if (blank) {
                    settingRepository.delete(key)
                } else {
                    settingRepository.saveSerializable(key, draft.copy(savedAtEpochMs = nowMs()))
                }
            }
        }
    }

    suspend fun clear(key: String) = withContext(serial) {
        // Join: if a write is already in flight, let it finish before deleting, so it can't land after.
        pendingSaves.remove(key)?.cancelAndJoin()
        photos.remove(key)
        settingRepository.delete(key)
    }

    /** Logout / account switch: drafts belong to the signed-in user. */
    suspend fun clearAll() = withContext(serial) {
        pendingSaves.values.forEach { it.cancelAndJoin() }
        pendingSaves.clear()
        photos.clear()
        settingRepository.readAll()
            .filter { it.key.startsWith(KEY_PREFIX) }
            .forEach { settingRepository.delete(it.key) }
    }

    companion object {
        fun newReviewKey(shopId: String) = "${KEY_PREFIX}new:$shopId"
        fun editReviewKey(reviewId: String) = "${KEY_PREFIX}edit:$reviewId"
    }
}
