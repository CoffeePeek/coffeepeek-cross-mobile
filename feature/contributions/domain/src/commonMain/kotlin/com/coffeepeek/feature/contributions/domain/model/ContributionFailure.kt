package com.coffeepeek.feature.contributions.domain.model

enum class FailureKind { SignInRequired, Forbidden, NotFound, Invalid, Upload, Other }

/**
 * Why a request failed. [serverMessage] is the server's own wording when it sent one; the UI shows it or a default text, never the transport error.
 * [Invalid] means the data was refused locally before any request.
 */
class ContributionFailure(val kind: FailureKind, val serverMessage: String? = null) : Exception(kind.name)
