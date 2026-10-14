package com.coffeepeek.feature.homecheckin.domain

enum class HomeCheckInField { Date, Method, Bean, Dose, Duration, Rating, Note, Drink, Photos, Visibility }

/** Presentation-safe failure. HTTP payloads and server exceptions stay in data. */
class HomeCheckInFailure(
    override val message: String,
    val fieldErrors: Map<HomeCheckInField, String> = emptyMap(),
) : Exception(message)
