package com.coffeepeek.admin.analytics

import com.coffeepeek.admin.utils.currentEpochMillis
import kotlin.random.Random
import kotlin.time.TimeSource

internal fun correlationId(prefix: String) = "$prefix-${currentEpochMillis().toString(36)}-${Random.nextInt(1, Int.MAX_VALUE).toString(36)}"

/** Criteria (including query and filter values) are kept only in this ViewModel-owned object. */
internal class SearchTracker(private val catalog: Catalog, private val source: ResultSource = ResultSource.List) {
    private var fingerprint: Any? = null
    private var generation: Long? = null
    private var current: SearchAttempt? = null
    private var revision = 0

    fun begin(criteria: Any, query: String, keys: List<String>, nearby: Boolean = false, sort: String = "relevance", citySlug: String? = null): SearchAttempt? {
        val service = Analytics.service ?: return null
        val identity = service.state.value.generation
        if (criteria == fingerprint && identity == generation) return current
        fingerprint = criteria
        generation = identity
        val expected = ++revision
        val length = query.trim().replace(Regex("\\s+"), " ").length.coerceAtMost(100)
        val properties = mutableMapOf<String, Any>("catalog" to catalog.wire, "search_id" to correlationId("search"),
            "source" to source.wire, "has_query" to (length > 0), "query_length" to length,
            "filter_keys" to keys.filter { it in filterKeys }.distinct().sorted(), "sort" to sort, "nearby" to nearby)
        citySlug?.let { properties["city_slug"] = it }
        return SearchAttempt(service, properties.toMap(), service.operationScope()) { revision == expected }.also { current = it }
    }

    fun open(entity: Entity, position: Int? = null, includeSearch: Boolean = true) {
        val props = mutableMapOf<String, Any>("entity_type" to entity.wire, "source" to source.wire)
        current?.id?.takeIf { includeSearch }?.let { props["search_id"] = it }
        position?.let { props["position"] = it }
        Analytics.service?.track(AnalyticsEvent.CATALOG_ITEM_OPENED, props)
        DetailEntryMemory.record(entity, source, current?.id?.takeIf { includeSearch })
    }
}

internal class SearchAttempt(private val service: AnalyticsService, private val props: Map<String, Any>, private val scope: AnalyticsScope, private val current: () -> Boolean) {
    private val started = TimeSource.Monotonic.markNow()
    private val accepted = service.track(AnalyticsEvent.CATALOG_SEARCH_STARTED, props, scope)
    private var settled = false
    val id: String? get() = (props["search_id"] as String).takeIf { accepted }
    fun results(count: Int) {
        if (settled || !accepted || !current()) return
        settled = true
        service.track(AnalyticsEvent.CATALOG_RESULTS_VIEWED, props + mapOf("result_count" to count.coerceAtLeast(0), "is_empty" to (count == 0), "duration_ms" to elapsed()), scope)
    }
    fun failed(error: SafeAnalyticsError) {
        if (settled || !accepted || !current() || error.category == "cancelled") return
        settled = true
        val category = when (error.category) {
            "authentication", "permission" -> "authorization"
            "network", "server", "validation" -> error.category
            "not_found", "rate_limited" -> "validation"
            else -> "unknown"
        }
        service.track(AnalyticsEvent.CATALOG_SEARCH_FAILED, props + mapOf("duration_ms" to elapsed(), "failure_category" to category) +
            error.status?.let { mapOf("http_status" to it) }.orEmpty(), scope)
    }
    private fun elapsed() = started.elapsedNow().inWholeMilliseconds.coerceAtLeast(0)
}

internal data class SafeAnalyticsError(val category: String, val status: Int? = null) {
    fun properties(): Map<String, Any> = mapOf("error_category" to category) + status?.takeIf { it in 100..599 }?.let { mapOf("status_code" to it) }.orEmpty()
}

internal class CheckInTracker(
    private var kind: CheckInKind,
    private val source: CheckInSource,
    private val edit: Boolean = false,
) {
    private val service = Analytics.service
    private val scope = service?.operationScope(if (edit) "check_in_edit" else "check_in_create")
    private val started = TimeSource.Monotonic.markNow()
    private val id = correlationId("checkin")
    private val steps = mutableSetOf<CheckInStep>()
    private val fields = mutableSetOf<String>()
    private var step = CheckInStep.Type
    private var completed = false
    private var attempt = 0
    val isFresh: Boolean get() = started.elapsedNow().inWholeHours < 24
    init { emit(AnalyticsEvent.CHECK_IN_FLOW_STARTED) }
    private fun props(): Map<String, Any> = mapOf("flow_id" to id, "kind" to kind.wire, "source" to source.wire, "mode" to if (edit) "edit" else "create")
    private fun elapsed() = started.elapsedNow().inWholeMilliseconds.coerceAtLeast(0)
    private fun emit(event: AnalyticsEvent, extra: Map<String, Any> = emptyMap()) {
        if (scope != null && !completed && isFresh) service?.track(event, props() + extra, scope)
    }
    fun kind(kind: CheckInKind) { this.kind = kind; emit(AnalyticsEvent.CHECK_IN_KIND_SELECTED) }
    fun step(step: CheckInStep) {
        this.step = step
        if (steps.add(step)) emit(AnalyticsEvent.CHECK_IN_STEP_VIEWED, mapOf("step" to step.wire, "duration_ms" to elapsed()))
    }
    fun field(field: String) {
        val category = field.takeIf { it in checkInFields } ?: "form"
        if (fields.add(category)) emit(AnalyticsEvent.CHECK_IN_FIELD_INTERACTED, mapOf("field" to category))
    }
    fun submit() {
        if (attempt > 0) retry(CheckInStage.Save)
        attempt++
        emit(AnalyticsEvent.CHECK_IN_SUBMIT_ATTEMPTED, mapOf("attempt" to attempt, "duration_ms" to elapsed()))
    }
    fun validation(fields: List<String>, server: Boolean = false) = emit(AnalyticsEvent.CHECK_IN_VALIDATION_FAILED,
        mapOf("attempt" to attempt, "fields" to fields.map { it.takeIf(checkInFields::contains) ?: "form" }.distinct().sorted(), "validation_source" to if (server) "server" else "client"))
    fun failed(stage: CheckInStage, error: SafeAnalyticsError) {
        if (error.category != "cancelled") emit(AnalyticsEvent.CHECK_IN_FAILED, mapOf("attempt" to attempt, "stage" to stage.wire, "duration_ms" to elapsed()) + error.properties())
    }
    fun retry(stage: CheckInStage) = emit(AnalyticsEvent.CHECK_IN_RETRY_CLICKED, mapOf("attempt" to attempt, "stage" to stage.wire))
    fun saved(visibility: String, hasPhotos: Boolean) {
        if (completed || !isFresh) return
        emit(AnalyticsEvent.CHECK_IN_SAVED, mapOf("attempt" to attempt, "duration_ms" to elapsed(), "visibility" to visibility, "has_photos" to hasPhotos))
        if (!edit && kind != CheckInKind.Undecided && scope != null) service?.track(AnalyticsEvent.CHECK_IN_CREATED,
            mapOf("flow_id" to id, "kind" to kind.wire, "source" to source.wire, "duration_ms" to elapsed(), "visibility" to visibility, "has_photos" to hasPhotos), scope)
        completed = true
    }
    fun cancel(reason: String) {
        emit(AnalyticsEvent.CHECK_IN_CANCELLED, mapOf("step" to step.wire, "reason" to reason, "duration_ms" to elapsed()))
        completed = true
    }
}

/** Only navigation metadata is retained, never an entity/user ID. Consumed by the destination. */
internal object DetailEntryMemory {
    private var pending: Triple<Entity, ResultSource, String?>? = null
    private var generation: Long? = null
    private var recorded = TimeSource.Monotonic.markNow()
    fun record(entity: Entity, source: ResultSource, search: String?) {
        pending = Triple(entity, source, search)
        generation = Analytics.service?.state?.value?.generation
        recorded = TimeSource.Monotonic.markNow()
    }
    fun consume(entity: Entity): Map<String, Any> {
        val entry = pending?.takeIf { it.first == entity && generation == Analytics.service?.state?.value?.generation && recorded.elapsedNow().inWholeMinutes < 1 }
        pending = null
        return mapOf("entity_type" to entity.wire, "entry_point" to (entry?.second?.wire ?: "direct")) +
            entry?.third?.let { mapOf("search_id" to it) }.orEmpty()
    }
}
