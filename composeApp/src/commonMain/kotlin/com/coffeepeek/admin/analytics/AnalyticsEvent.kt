package com.coffeepeek.admin.analytics

/** Version 1 of the shared web/mobile wire contract. No arbitrary event names. */
internal enum class AnalyticsEvent(val required: String, val optional: String = "") {
    FEATURE_EXPOSED("feature placement"),
    SIGN_IN_COMPLETED("method entry_point"),
    SIGN_UP_COMPLETED("method"),
    AUTH_GATE_SHOWN("feature"),
    CATALOG_SEARCH_STARTED(SEARCH),
    CATALOG_RESULTS_VIEWED("$SEARCH result_count is_empty duration_ms"),
    CATALOG_SEARCH_FAILED("$SEARCH duration_ms failure_category", "http_status"),
    CATALOG_ITEM_OPENED("entity_type source", "search_id position"),
    FILTERS_OPENED("catalog placement"),
    FILTERS_APPLIED("catalog filter_keys changed_filter_keys action"),
    MAP_INTERACTION("action", "result_count duration_ms enabled"),
    GEOLOCATION_RESULT("placement trigger outcome", "duration_ms"),
    ENTITY_DETAIL_VIEWED("entity_type entry_point", "search_id"),
    SHOP_CONTENT_INTERACTED("section action"),
    SHOP_ACTION_CLICKED("action entry_point"),
    ENTITY_CONTENT_INTERACTED("entity_type section action"),
    FAVORITE_ATTEMPTED("entity_type", "is_favorite"),
    FAVORITE_CHANGED("entity_type is_favorite", "duration_ms resumed_after_auth favorite_storage"),
    FAVORITE_FAILED("entity_type duration_ms error_category", "status_code favorite_storage"),
    FAVORITES_OPENED("entry_point"),
    CHECK_IN_START_ATTEMPTED("source"),
    CHECK_IN_FLOW_STARTED(FLOW),
    CHECK_IN_KIND_SELECTED(FLOW),
    CHECK_IN_STEP_VIEWED("$FLOW step duration_ms"),
    CHECK_IN_FIELD_INTERACTED("$FLOW field"),
    CHECK_IN_SUBMIT_ATTEMPTED("$FLOW attempt duration_ms"),
    CHECK_IN_VALIDATION_FAILED("$FLOW attempt fields validation_source"),
    CHECK_IN_FAILED("$FLOW attempt stage duration_ms error_category", "status_code"),
    CHECK_IN_RETRY_CLICKED("$FLOW attempt stage"),
    CHECK_IN_SAVED("$FLOW attempt duration_ms visibility has_photos"),
    CHECK_IN_CREATED("kind visibility has_photos", "flow_id source duration_ms"),
    CHECK_IN_CANCELLED("$FLOW step reason duration_ms"),
    FEED_VIEWED("result_count is_empty"),
    FEED_ACTION_CLICKED("action trigger"),
    FEED_ITEM_INTERACTED("action check_in_kind"),
    API_REQUEST_COMPLETED("category method outcome duration_ms auth_retried", "status_code error_category"),
    APP_DOWNLOAD_CLICKED("channel");

    val wire: String get() = name.lowercase()
}

private const val SEARCH = "catalog search_id source has_query query_length filter_keys sort nearby"
private const val FLOW = "flow_id kind mode source"

internal enum class Entity(val wire: String) { Shop("coffee_shop"), Roaster("roaster"), Coffee("coffee") }
internal enum class Catalog(val wire: String) { Discovery("discovery"), Shops("shops"), Roasters("roasters"), Coffees("coffees") }
internal enum class ResultSource(val wire: String) { List("list"), Map("map"), Favorites("favorites"), Related("related"), Direct("direct") }
internal enum class CheckInKind(val wire: String) { Undecided("undecided"), Visit("visit"), Home("home") }
internal enum class CheckInSource(val wire: String) { Global("global_create"), Shop("shop_detail"), Direct("direct"), Diary("my_check_ins") }
internal enum class CheckInStep(val wire: String) { Type("type_picker"), Shop("shop_picker"), Editor("editor"), Bean("bean_picker") }
internal enum class CheckInStage(val wire: String) { ShopSearch("shop_search"), ShopDetail("shop_detail"), Load("load"), Photo("photo_upload"), Save("save"), Bean("bean_search") }

internal val pageTypes = setOf("landing", "discovery", "shops", "map", "coffees", "roasters", "feed", "my_check_ins", "profile", "login", "register", "settings", "privacy", "terms", "error", "check_in_create", "check_in_edit", "shop_edit", "shop_detail", "roaster_detail", "coffee_detail", "user_profile", "contributions", "other")
internal val filterKeys = setOf("city", "tags", "excludeTags", "roasters", "isOpen", "minRating", "isNew", "equipments", "beans", "brewMethods", "priceRange", "type", "visitedOnly", "favoritesOnly", "nearby", "radiusKm", "countries", "productKind", "productForm", "brew", "caffeine", "roast", "acidity", "processing", "fermentation", "taste", "composition", "weightGrams", "currency", "minPrice", "maxPrice", "availabilityScope", "availableOnly", "menu.brew", "menu.currency", "menu.minPrice", "menu.maxPrice", "menu.volumeMl", "budget.currency", "budget.minDrinkPrice", "budget.maxDrinkPrice", "budget.drinkVolumeMl", "budget.minCoffeePrice", "budget.maxCoffeePrice", "budget.coffeeWeightGrams")
internal val checkInFields = setOf("note", "rating_coffee", "rating_service", "rating_place", "rating", "date", "shop", "drink", "visibility", "photos", "brew_method", "bean", "dose", "brew_duration", "rating_taste", "form")
internal val errorCategories = setOf("validation", "authentication", "permission", "not_found", "rate_limited", "server", "network", "cancelled", "unknown")

/** Fails closed, including unknown keys and enums. Values of text/IDs never cross this boundary. */
internal object AnalyticsSchema {
    private val commonEnums = mapOf(
        "catalog" to Catalog.entries.map { it.wire }.toSet(),
        "entity_type" to Entity.entries.map { it.wire }.toSet(),
        "mode" to setOf("create", "edit"), "visibility" to setOf("Private", "Public", "unknown"),
        "step" to CheckInStep.entries.map { it.wire }.toSet(),
        "stage" to CheckInStage.entries.map { it.wire }.toSet(),
        "error_category" to errorCategories,
        "failure_category" to setOf("network", "authorization", "validation", "server", "unknown"),
        "validation_source" to setOf("client", "server"),
        "sort" to setOf("name_asc", "relevance", "distance_asc", "rating_desc", "available_coffees_desc", "price_asc", "price_desc", "newest", "unknown"),
        "method" to setOf("GET", "POST", "PUT", "PATCH", "DELETE"),
        "category" to setOf("authentication", "favorites", "check_ins", "photos", "coffees", "roasters", "shops", "feed", "catalog", "profile", "other"),
        "channel" to setOf("google-play", "app-store", "apk"),
        "field" to checkInFields, "reason" to setOf("close", "back", "map"),
        "favorite_storage" to setOf("local", "server"),
    )
    private val features = setOf("search", "filters", "shop_catalog", "roaster_catalog", "coffee_catalog", "map", "favorites", "favorites_filter", "visited_filter", "check_in", "home_check_in", "directions", "share", "shop_roasters", "shop_hours", "shop_contacts", "shop_photos", "shop_menu", "coffee_offers", "similar_coffees", "coffee_photos", "coffee_roaster", "roaster_assortment", "roaster_shops", "roaster_photos", "roaster_contacts", "feed", "feed_shop_check_in", "feed_home_check_in", "edit_shop", "unspecified", "feed_like", "report_issue")
    private val placements = pageTypes + setOf("catalog", "home", "coffee_shop_card", "roaster_card", "catalog_card", "global_create", "check_in_type_picker")

    fun validate(event: AnalyticsEvent, properties: Map<String, Any>): Map<String, Any>? {
        val required = event.required.split(' ').toSet()
        val optional = event.optional.split(' ').filter(String::isNotEmpty).toSet() +
            if (event in searchEvents) setOf("city_slug") else emptySet()
        if (!properties.keys.containsAll(required) || (properties.keys - required - optional).isNotEmpty()) return null
        val clean = properties.mapValues { (key, value) ->
            when (key) {
                "filter_keys", "changed_filter_keys", "fields" -> {
                    val list = value as? List<*> ?: return null
                    val allowed = if (key == "fields") checkInFields else filterKeys
                    if (list.any { it !is String || it !in allowed }) return null
                    list.filterIsInstance<String>().distinct().sorted()
                }
                "duration_ms", "result_count", "query_length", "position", "attempt", "status_code", "http_status" -> {
                    if (value !is Int && value !is Long) return null
                    val n = (value as Number).toLong()
                    if (n < 0 || key == "query_length" && n > 100 || key == "position" && n < 1 ||
                        key in setOf("status_code", "http_status") && n !in 100..599) return null
                    value
                }
                "has_query", "nearby", "is_empty", "is_favorite", "resumed_after_auth", "has_photos", "auth_retried", "enabled" -> value as? Boolean ?: return null
                "search_id" -> (value as? String)?.takeIf { it.matches(Regex("search-[a-z0-9]{1,12}-[a-z0-9]{1,12}")) } ?: return null
                "flow_id" -> (value as? String)?.takeIf { it.matches(Regex("checkin-[a-z0-9-]{1,70}")) } ?: return null
                "city_slug" -> (value as? String)?.takeIf { it.length <= 80 && it.matches(Regex("[a-z0-9-]+")) } ?: return null
                else -> {
                    val text = value as? String ?: return null
                    val allowed = enums(event, key) ?: return null
                    if (text !in allowed) return null
                    text
                }
            }
        }
        if (clean["has_query"] != null && clean["has_query"] != ((clean["query_length"] as Number).toInt() > 0)) return null
        if (clean["is_empty"] != null && clean["is_empty"] != ((clean["result_count"] as Number).toInt() == 0)) return null
        return clean
    }

    private fun enums(event: AnalyticsEvent, key: String): Set<String>? = when (key) {
        "feature" -> features
        "placement" -> when (event) {
            AnalyticsEvent.FILTERS_OPENED -> setOf("desktop", "mobile")
            AnalyticsEvent.GEOLOCATION_RESULT -> setOf("discovery", "filters", "map")
            else -> placements
        }
        "method" -> if (event in setOf(AnalyticsEvent.SIGN_IN_COMPLETED, AnalyticsEvent.SIGN_UP_COMPLETED))
            if (event == AnalyticsEvent.SIGN_UP_COMPLETED) setOf("password") else setOf("password", "google", "apple") else commonEnums[key]
        "entity_type" -> when (event) {
            AnalyticsEvent.FAVORITE_ATTEMPTED, AnalyticsEvent.FAVORITE_CHANGED, AnalyticsEvent.FAVORITE_FAILED -> setOf("coffee_shop", "roaster")
            AnalyticsEvent.ENTITY_CONTENT_INTERACTED -> setOf("coffee", "roaster")
            else -> commonEnums[key]
        }
        "kind" -> if (event == AnalyticsEvent.CHECK_IN_CREATED) setOf("visit", "home") else setOf("undecided", "visit", "home")
        "check_in_kind" -> setOf("shop", "home")
        "source" -> if (event.name.startsWith("CHECK_IN")) CheckInSource.entries.map { it.wire }.toSet()
            else if (event in searchEvents) setOf("list", "map", "favorites") else setOf("list", "map", "favorites", "related")
        "entry_point" -> when (event) {
            AnalyticsEvent.SIGN_IN_COMPLETED -> setOf("login", "register")
            AnalyticsEvent.FAVORITES_OPENED -> setOf("profile")
            AnalyticsEvent.SHOP_ACTION_CLICKED -> setOf("shop_detail", "shop_contacts", "mobile_actions", "shop_header")
            else -> ResultSource.entries.map { it.wire }.toSet()
        }
        "section" -> if (event == AnalyticsEvent.SHOP_CONTENT_INTERACTED) setOf("photos", "menu", "hours", "roasters")
            else setOf("photos", "description", "roaster", "similar_coffees", "assortment", "shops", "contacts")
        "trigger" -> if (event == AnalyticsEvent.GEOLOCATION_RESULT) setOf("automatic", "user") else setOf("button", "gesture", "scroll")
        "outcome" -> if (event == AnalyticsEvent.GEOLOCATION_RESULT) setOf("available", "unavailable") else setOf("success", "failure")
        "action" -> when (event) {
            AnalyticsEvent.FILTERS_APPLIED -> setOf("apply", "reset")
            AnalyticsEvent.MAP_INTERACTION -> setOf("expanded", "collapsed", "loaded", "load_failed", "marker_selected", "search_selected", "zones_toggled", "zoomed", "nearby_loaded", "nearby_failed")
            AnalyticsEvent.SHOP_CONTENT_INTERACTED -> setOf("open", "close", "select")
            AnalyticsEvent.SHOP_ACTION_CLICKED -> setOf("directions", "share", "phone_copy", "phone_call", "email", "website", "instagram", "check_in", "edit", "report_issue")
            AnalyticsEvent.ENTITY_CONTENT_INTERACTED -> setOf("open", "close", "select", "view_all", "website", "instagram", "share")
            AnalyticsEvent.FEED_ACTION_CLICKED -> setOf("refresh", "load_more", "retry")
            AnalyticsEvent.FEED_ITEM_INTERACTED -> setOf("author", "shop", "photo", "like", "unlike", "home_detail")
            else -> null
        }
        else -> commonEnums[key]
    }

    val searchEvents = setOf(AnalyticsEvent.CATALOG_SEARCH_STARTED, AnalyticsEvent.CATALOG_RESULTS_VIEWED, AnalyticsEvent.CATALOG_SEARCH_FAILED)
}
